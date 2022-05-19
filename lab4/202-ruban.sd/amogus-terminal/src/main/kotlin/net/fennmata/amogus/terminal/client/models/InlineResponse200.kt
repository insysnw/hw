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

package net.fennmata.amogus.terminal.client.models

import com.squareup.moshi.Json

/**
 *
 *
 * @param role
 * @param location
 * @param identity
 */

data class InlineResponse200(

    @Json(name = "role")
    val role: Role,

    @Json(name = "location")
    val location: MoveTo,

    @Json(name = "identity")
    val identity: kotlin.String

)
