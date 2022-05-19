# net.fennmata.amogus.terminal.client - Kotlin client library for Amogus Terminal

## Requires

* Kotlin 1.4.30
* Gradle 6.8.3

## Build

First, create the gradle wrapper script:

```
gradle wrapper
```

Then, run:

```
./gradlew check assemble
```

This runs all tests and packages the library.

## Features/Implementation Notes

* Supports JSON inputs/outputs, File inputs, and Form inputs.
* Supports collection formats for query parameters: csv, tsv, ssv, pipes.
* Some Kotlin and Java types are fully qualified to avoid conflicts with types defined in OpenAPI definitions.
* Implementation of ApiClient is intended to reduce method counts, specifically to benefit Android targets.

<a name="documentation-for-api-endpoints"></a>
## Documentation for API Endpoints

All URIs are relative to *http://localhost*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*MainApi* | [**getMyself**](docs/MainApi.md#getmyself) | **GET** /user/me | Returns your status
*MainApi* | [**getNewUser**](docs/MainApi.md#getnewuser) | **GET** /user/new | Get the initial user context
*MainApi* | [**postQuery**](docs/MainApi.md#postquery) | **POST** /query | Run a command


<a name="documentation-for-models"></a>
## Documentation for Models

 - [net.fennmata.amogus.terminal.client.models.FilesList](docs/FilesList.md)
 - [net.fennmata.amogus.terminal.client.models.InlineResponse200](docs/InlineResponse200.md)
 - [net.fennmata.amogus.terminal.client.models.InlineResponse2001](docs/InlineResponse2001.md)
 - [net.fennmata.amogus.terminal.client.models.KillResult](docs/KillResult.md)
 - [net.fennmata.amogus.terminal.client.models.MoveTo](docs/MoveTo.md)
 - [net.fennmata.amogus.terminal.client.models.Notification](docs/Notification.md)
 - [net.fennmata.amogus.terminal.client.models.Query](docs/Query.md)
 - [net.fennmata.amogus.terminal.client.models.Role](docs/Role.md)
 - [net.fennmata.amogus.terminal.client.models.UsersList](docs/UsersList.md)
 - [net.fennmata.amogus.terminal.client.models.UsersListUsers](docs/UsersListUsers.md)


<a name="documentation-for-authorization"></a>
## Documentation for Authorization

<a name="sus"></a>
### sus

- **Type**: API key
- **API key parameter name**: Identity
- **Location**: HTTP header

