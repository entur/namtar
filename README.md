# namtar [![CircleCI](https://circleci.com/gh/entur/namtar/tree/master.svg?style=svg)](https://circleci.com/gh/entur/namtar/tree/master)

Application responsible for producing and providing persistent DatedServiceJourneyIds from NeTEx-data

Healthcheck:
```
http://<server>:<port>/path/health/ready

http://<server>:<port>/path/health/up
```

# Usage
Lookup serviceJourneyId and date
``` 
http://<server>:<port>/path/api/{serviceJourneyId}/{version}/{date}

{version} is either number (0-n) or ´latest´

``` 

Reverse lookup to resolve serviceJourney, version and date from DatedServiceJourney
``` 
http://<server>:<port>/path/api/dated/{datedServiceJourneyId}
``` 


Reverse lookup to resolve all serviceJourneys that share the provided originalDatedServiceJourney
``` 
http://<server>:<port>/path/api/original/{originalDatedServiceJourneyId}
``` 


Example-response:
``` 
{
    "serviceJourneyId": "XXX:ServiceJourney:1-234-567",
    "departureDate": "2018-12-24",
    "privateCode": "321",
    "departureTime": "16:55",
    "lineRef": "XXX:Line:256",
    "version": 0,
    "datedServiceJourneyId": "ENT:DatedServiceJourney:9876",
    "publicationTimestamp": "2018-07-15T20:19:39",
    "sourceFileName": "netex_export.zip",
    "originalDatedServiceJourneyId": "ENT:DatedServiceJourney:1234"
}
```
