## Weather forecast cache for specific locations at specific time.

### Run project
- Run redis docker container:
  ```
  docker run -d --name redis-stack -p 6379:6379 -p 8001:8001 redis/redis-stack:latest
  ```
- cd ~/forecast_cache
  ```
  mvn compile
  ```
  ```
  mvn exec:java -Dexec.mainClass=org.example.ForecastCache
  ```
- Display cached data: http://localhost:8001/redis-stack/browser    

### REST API endpoints for interacting with the forecast cache.
* [Forecast REST API](https://github.com/denysbiliaiev/forecast)

### Improvements
- Test coverage
- Put hard coded string in the environment variables
- Optimize http traffic (request frequency)
- Optimize forecast search in time series
- Add health check api.met.no
- Check api.met.no response status
- Add logging
- Add application health check endpoint
- Containerize an application



