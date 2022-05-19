# MainApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getMyself**](MainApi.md#getMyself) | **GET** /user/me | Returns your status
[**getNewUser**](MainApi.md#getNewUser) | **GET** /user/new | Get the initial user context
[**postQuery**](MainApi.md#postQuery) | **POST** /query | Run a command


<a name="getMyself"></a>
# **getMyself**
> InlineResponse2001 getMyself()

Returns your status

### Example
```kotlin
// Import classes:
//import net.fennmata.amogus.terminal.client.infrastructure.*
//import net.fennmata.amogus.terminal.client.models.*

val apiInstance = MainApi()
try {
    val result : InlineResponse2001 = apiInstance.getMyself()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MainApi#getMyself")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MainApi#getMyself")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**InlineResponse2001**](InlineResponse2001.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="getNewUser"></a>
# **getNewUser**
> InlineResponse200 getNewUser()

Get the initial user context

### Example
```kotlin
// Import classes:
//import net.fennmata.amogus.terminal.client.infrastructure.*
//import net.fennmata.amogus.terminal.client.models.*

val apiInstance = MainApi()
try {
    val result : InlineResponse200 = apiInstance.getNewUser()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MainApi#getNewUser")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MainApi#getNewUser")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**InlineResponse200**](InlineResponse200.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="postQuery"></a>
# **postQuery**
> KillResult postQuery(requestBody)

Run a command

### Example
```kotlin
// Import classes:
//import net.fennmata.amogus.terminal.client.infrastructure.*
//import net.fennmata.amogus.terminal.client.models.*

val apiInstance = MainApi()
val requestBody : Query =  // Query | The command line to run
try {
    val result : KillResult = apiInstance.postQuery(requestBody)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MainApi#postQuery")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MainApi#postQuery")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **requestBody** | [**Query**](Query.md)| The command line to run |

### Return type

[**KillResult**](KillResult.md)

### Authorization


Configure sus:
    ApiClient.apiKey["Identity"] = ""
    ApiClient.apiKeyPrefix["Identity"] = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

