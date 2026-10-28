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
import com.wissli.app.data.auth.LastAccountLocalDataSource
import com.wissli.app.data.auth.SharedPreferencesLastAccountDataSource
import com.wissli.app.features.login.presentation.LoginViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authModule =
    module {
        single<FirebaseAuth> { Firebase.auth }
        single<FirebaseFirestore> { Firebase.firestore }

        single<GoogleAuthDataSource> {
            CredentialManagerGoogleAuthDataSource(webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID)
        }
        single<LastAccountLocalDataSource> {
            SharedPreferencesLastAccountDataSource(context = androidContext())
        }
        single<AuthRepository> {
            FirebaseAuthRepository(
                firebaseAuth = get(),
                firestore = get(),
                lastAccountLocalDataSource = get(),
            )
        }

        viewModelOf(::LoginViewModel)
        viewModelOf(::AuthGateViewModel)
    }
