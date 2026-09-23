package com.wissli.app.di

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.wissli.app.BuildConfig
import com.wissli.app.core.navigation.AuthGateViewModel
import com.wissli.app.data.auth.AuthRepository
import com.wissli.app.data.auth.CredentialManagerGoogleAuthDataSource
import com.wissli.app.data.auth.FirebaseAuthRepository
import com.wissli.app.data.auth.GoogleAuthDataSource
import com.wissli.app.feature.auth.login.LoginViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Feature-Modul für Auth. Wird in [appModule] per includes() eingehängt statt alles in einem
 * einzigen riesigen Modul zu sammeln — künftige Feature-Module (Questions, Family, Challenges)
 * folgen demselben Muster: eigene di/<Feature>Module.kt, in AppModule includen.
 */
val authModule =
    module {
        single<FirebaseAuth> { Firebase.auth }
        single<FirebaseFirestore> { Firebase.firestore }

        single<GoogleAuthDataSource> {
            CredentialManagerGoogleAuthDataSource(webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID)
        }
        single<AuthRepository> { FirebaseAuthRepository(firebaseAuth = get(), firestore = get()) }

        viewModelOf(::LoginViewModel)
        viewModelOf(::AuthGateViewModel)
    }
