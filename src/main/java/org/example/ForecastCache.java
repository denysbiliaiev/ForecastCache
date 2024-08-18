package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import redis.clients.jedis.JedisPooled;

import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForecastCache {

    /* TODO put host, port in the environment variables */
    private static final JedisPooled jedis = new JedisPooled("localhost", 6379);
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    /* 55(milliseconds) ≈ 18 requests per second (api.met.no limit=20) */
    private static final int REQUESTS_FREQUENCY = 55;

    /* TODO optimize HTTP traffic (10000 used for test)*/
    /* (60 * 60 * 2 * 1000)milliseconds = 2hours */
    private static final int CACHE_UPDATE_FREQUENCY = 10000;

    /* (60 * 60 * 3)seconds = 3hours */
    private static final int FORECAST_CACHE_EXPIRATION = 30;

    public static void main(String[] args) throws IOException, InterruptedException, ParseException {
        boolean isСached = true;

        while (isСached) {
            /* TODO check api.met.no health status */

            List<Event> events = ForecastCache.receiveEventsForNext7Days();

            /* TODO add logging */
            System.out.println("Receiving forecast...");

            for (Event event : events) {
                String cacheKey = event.start + ";" + event.lat + ";" + event.lon;

                String lastCachedForecast = jedis.get(cacheKey);

                /* TODO put url, headers value, in the environment variables */
                Builder forecastRequest = new Request.Builder()
                        .url("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=" + event.lat + "&lon=" + event.lon)
                        .addHeader("User-Agent", "Denys denys.biliaiev@gmail.com");

                /*
                    Forecast for the location is always pull for the first time,
                    if the second and further, then only if it has been updated.
                */
                if (lastCachedForecast != null) {
                    String metForecastLastModified = mapper.readValue(jedis.get(cacheKey), ForecastDto.class).getLastModified();
                    /* If the target forecast has not changed since the last modified, then the response body will be empty. */
                    forecastRequest.addHeader("If-Modified-Since", metForecastLastModified);
                }

                Response forecastResponse = client.newCall(forecastRequest.build()).execute();

                /* TODO check response status */

                if (forecastResponse.body().contentLength() != 0) {
                    MetForecast metResponse = mapper.readValue(forecastResponse.body().byteStream(), MetForecast.class);

                    Map<String, ForecastDto> forecastMapper = createForecastMapper(metResponse.properties.timeseries, forecastResponse.header("Last-Modified"));
                    ForecastDto forecastForEventPeriode = findForecastForEventPeriode(event, forecastMapper);

                    jedis.setex(cacheKey, FORECAST_CACHE_EXPIRATION, mapper.writeValueAsString(forecastForEventPeriode));

                    System.out.println(forecastForEventPeriode);
                }

                Thread.sleep(REQUESTS_FREQUENCY);
            }

            Thread.sleep(CACHE_UPDATE_FREQUENCY);
        }
    }

    public static Map<String, ForecastDto> createForecastMapper(List<Timeserie> timeseries, String lastModified) {
        Map<String, ForecastDto> forecastMapper = new HashMap<>();

        for (Timeserie timeserie : timeseries) {
            forecastMapper.put(timeserie.time, new ForecastDto()
                    .setAirTemperature(timeserie.data.instant.details.air_temperature)
                    .setWindSpeed(timeserie.data.instant.details.wind_speed)
                    .setLastModified(lastModified));
        }

        return forecastMapper;
    }

    public static ForecastDto findForecastForEventPeriode(Event event, Map<String, ForecastDto> forecast) throws ParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        LocalDateTime date = LocalDateTime.parse(event.start, formatter);

        /* TODO optimize forecast search in time series */
        String appropriateDateTime = forecast
            .keySet()
            .stream()
                .min((a, b) -> {
                    long modul1 = Math.abs(Duration.between(date, LocalDateTime.parse(a, formatter)).toMinutes());
                    long modul2 = Math.abs(Duration.between(date, LocalDateTime.parse(b, formatter)).toMinutes());
                    return Long.compare(modul1, modul2);
                })
                .orElseGet(() -> event.start);

        return forecast.get(appropriateDateTime);
    }

    public static List<Event> receiveEventsForNext7Days() {
        /* TODO receive events from event service. */
        Event event1 = new Event("59.88", "10.81", "2024-08-23T12:00:00Z", "2024-08-23T14:00:00Z");
        Event event2 = new Event("59.88", "10.81", "2024-08-24T14:59:00Z", "2024-08-24T16:30:00Z");
        Event event3 = new Event("63.43", "10.35", "2024-08-19T15:30:00Z", "2024-08-19T16:30:00Z");
        Event event4 = new Event("68.09", "13.23", "2024-08-20T10:50:00Z", "2024-08-20T12:30:00Z");
        Event event5 = new Event("71.09", "25.38", "2024-08-24T16:00:00Z", "2024-08-24T16:30:00Z");
        List<Event> events = new ArrayList<>();

        events.add(event1);
        events.add(event2);
        events.add(event3);
        events.add(event4);
        events.add(event5);

        return events;
    }
}