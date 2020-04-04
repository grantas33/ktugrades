package org.ktugrades.backend

import org.ktugrades.common.SubscriptionPayload
import io.ktor.client.HttpClient
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPublicKeySpec
import java.security.KeyFactory
import java.security.PublicKey
import java.util.*
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

fun SubscriptionPayload.getKeyAsBytes(): ByteArray = Base64.getDecoder().decode(this.key)

fun SubscriptionPayload.generatePublicKey(): PublicKey? {
    val kf: KeyFactory = KeyFactory.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
    val ecSpec = ECNamedCurveTable.getParameterSpec("secp256r1")
    val point = ecSpec.curve.decodePoint(getKeyAsBytes())
    val pubSpec = ECPublicKeySpec(point, ecSpec)

    return kf.generatePublic(pubSpec)
}

data class CoroutineClient(
    val client: HttpClient
) : AbstractCoroutineContextElement(CoroutineClient) {
    companion object Key : CoroutineContext.Key<CoroutineClient>
}

suspend fun getClient() = coroutineContext[CoroutineClient]!!.client
