/**
 * Amogus Terminal
 *
 * This is an OpenAPI version of the [Amogus Terminal](https://github.com/lunakoly/NetLab3) protocol.
 *
 * The version of the OpenAPI document: 1.0.0
 * Contact: lunyak.na@edu.spbstu.ru
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package net.fennmata.amogus.terminal.client.apis

import net.fennmata.amogus.terminal.client.infrastructure.ApiClient
import net.fennmata.amogus.terminal.client.infrastructure.ApiResponse
import net.fennmata.amogus.terminal.client.infrastructure.ClientError
import net.fennmata.amogus.terminal.client.infrastructure.ClientException
import net.fennmata.amogus.terminal.client.infrastructure.MultiValueMap
import net.fennmata.amogus.terminal.client.infrastructure.RequestConfig
import net.fennmata.amogus.terminal.client.infrastructure.RequestMethod
import net.fennmata.amogus.terminal.client.infrastructure.ResponseType
import net.fennmata.amogus.terminal.client.infrastructure.ServerError
import net.fennmata.amogus.terminal.client.infrastructure.ServerException
import net.fennmata.amogus.terminal.client.infrastructure.Success
import net.fennmata.amogus.terminal.client.models.InlineResponse200
import net.fennmata.amogus.terminal.client.models.InlineResponse2001
import net.fennmata.amogus.terminal.client.models.Query
import net.fennmata.amogus.terminal.client.models.ServerResponse
import java.io.IOException

class MainApi(basePath: kotlin.String = defaultBasePath) : ApiClient(basePath) {
    companion object {
        @JvmStatic
        val defaultBasePath: String by lazy {
            System.getProperties().getProperty(ApiClient.baseUrlKey, "http://localhost")
        }
    }

    /**
     * Returns your status
     *
     * @return InlineResponse2001
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun getMyself(): InlineResponse2001 {
        val localVarResponse = getMyselfWithHttpInfo()

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as InlineResponse2001
            ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
            ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
            ResponseType.ClientError -> {
                val localVarError = localVarResponse as ClientError<*>
                throw ClientException("Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
            }
            ResponseType.ServerError -> {
                val localVarError = localVarResponse as ServerError<*>
                throw ServerException("Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
            }
        }
    }

    /**
     * Returns your status
     *
     * @return ApiResponse<InlineResponse2001?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun getMyselfWithHttpInfo(): ApiResponse<InlineResponse2001?> {
        val localVariableConfig = getMyselfRequestConfig()

        return request<Unit, InlineResponse2001>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation getMyself
     *
     * @return RequestConfig
     */
    fun getMyselfRequestConfig(): RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.GET,
            path = "/user/me",
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

    /**
     * Get the initial user context
     *
     * @return InlineResponse200
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun getNewUser(): InlineResponse200 {
        val localVarResponse = getNewUserWithHttpInfo()

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as InlineResponse200
            ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
            ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
            ResponseType.ClientError -> {
                val localVarError = localVarResponse as ClientError<*>
                throw ClientException("Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
            }
            ResponseType.ServerError -> {
                val localVarError = localVarResponse as ServerError<*>
                throw ServerException("Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
            }
        }
    }

    /**
     * Get the initial user context
     *
     * @return ApiResponse<InlineResponse200?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun getNewUserWithHttpInfo(): ApiResponse<InlineResponse200?> {
        val localVariableConfig = getNewUserRequestConfig()

        return request<Unit, InlineResponse200>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation getNewUser
     *
     * @return RequestConfig
     */
    fun getNewUserRequestConfig(): RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.GET,
            path = "/user/new",
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

    /**
     * Run a command
     *
     * @param requestBody The command line to run
     * @return ServerResponse
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class, UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun postQuery(requestBody: Query): ServerResponse {
        val localVarResponse = postQueryWithHttpInfo(requestBody = requestBody)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as ServerResponse
            ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
            ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
            ResponseType.ClientError -> {
                val localVarError = localVarResponse as ClientError<*>
                throw ClientException("Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
            }
            ResponseType.ServerError -> {
                val localVarError = localVarResponse as ServerError<*>
                throw ServerException("Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
            }
        }
    }

    /**
     * Run a command
     *
     * @param requestBody The command line to run
     * @return ApiResponse<ServerResponse?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun postQueryWithHttpInfo(requestBody: Query): ApiResponse<ServerResponse?> {
        val localVariableConfig = postQueryRequestConfig(requestBody = requestBody)

        return request<Query, ServerResponse>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation postQuery
     *
     * @param requestBody The command line to run
     * @return RequestConfig
     */
    fun postQueryRequestConfig(requestBody: Query): RequestConfig<Query> {
        val localVariableBody = requestBody
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        localVariableHeaders["Content-Type"] = "application/json"
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.POST,
            path = "/query",
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

}
