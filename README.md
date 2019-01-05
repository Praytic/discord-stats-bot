### Arguments

#### Application Arguments

#### Task Arguments

### Caching

Every time-consuming transcation should be cached in order to improve user experience with the bot. Redis is used as a caching tool. Long transactions are refreshed in cache periodically (by default daily, see [parameters](#application-arguments) for more info).