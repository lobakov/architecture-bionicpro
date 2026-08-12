package com.bionicpro.auth.service

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SessionService {

    fun createSessionId(): String = UUID.randomUUID().toString()

    fun rotateSessionId(): String = UUID.randomUUID().toString()
}
