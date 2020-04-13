package org.ktugrades.backend

import io.ktor.client.HttpClient
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.InputStreamReader
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PublicKey
import java.util.*
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

fun String.asBase64Encoded(): ByteArray = Base64.getDecoder().decode(this)

fun ByteArray.asPublicKey(): PublicKey? {
    val kf: KeyFactory = KeyFactory.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
    val ecSpec = ECNamedCurveTable.getParameterSpec("secp256r1")
    val point = ecSpec.curve.decodePoint(this)
    val pubSpec = ECPublicKeySpec(point, ecSpec)

    return kf.generatePublic(pubSpec)
}

fun getVapidKeyPair(): KeyPair? {
    val reader = InputStreamReader(object {}.javaClass.getResourceAsStream("/vapid_private.pem"))
    val pemParser = PEMParser(reader)
    val keyPair = pemParser.readObject() as PEMKeyPair

    return JcaPEMKeyConverter().getKeyPair(keyPair)
}

data class CoroutineClient(
    val client: HttpClient
) : AbstractCoroutineContextElement(CoroutineClient) {
    companion object Key : CoroutineContext.Key<CoroutineClient>
}

suspend fun getClient() = coroutineContext[CoroutineClient]!!.client

data class MarkInformation(
    val moduleCode: String,
    val semesterNumber: String,
    val typeId: String?,
    val week: String,
    val marks: List<String>
)

data class Module (
    val code: String,
    val semesterNumber: String,
    val title: String,
    val professor: String
)
