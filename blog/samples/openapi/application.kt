package com.example

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.features.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import com.fasterxml.jackson.databind.*
import io.ktor.jackson.*
import io.ktor.auth.*
import kotlin.reflect.*
import java.util.*
import io.ktor.swagger.experimental.*
import io.ktor.auth.jwt.*
import com.auth0.jwt.*
import com.auth0.jwt.algorithms.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    val myjwt = MyJWT(secret = environment.config.property("jwt.secret").getString())

    val client = HttpClient(Apache) {
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    install(Authentication) {
        // For accessing the protected API resources, you must have received a a valid JWT token after registering or logging in. This JWT token must then be used for all protected resources by passing it in via the 'Authorization' header.\n\nA JWT token is generated by the API by either registering via /users or logging in via /users/login.\n\nThe following format must be in the 'Authorization' header :\n\n    Token: xxxxxx.yyyyyyy.zzzzzz\n    \n
        // @TODO: Please, edit the application.conf # jwt.secret property and provide a secure random value for it
        jwt("Token") {
            authSchemes("Bearer", "Token")
            verifier(myjwt.verifier)
            validate {
                UserIdPrincipal(it.payload.getClaim("name").asString())
            }
        }
    }

    routing {
        install(StatusPages) {
            exception<HttpException> {  cause ->
                call.respond(cause.code, cause.description)
            }
        }

        get("/json/jackson") {
            call.respond(mapOf("hello" to "world"))
        }

        registerRoutes(ConduitAPIServer(myjwt))
    }
}

open class MyJWT(val secret: String) {
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier = JWT.require(algorithm).build()
    fun sign(name: String): String = JWT.create().withClaim("name", name).sign(algorithm)
}

