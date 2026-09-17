package com.denariidolor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.denariidolor.databinding.ActivityMainBinding
import com.denariidolor.presentation.ui.auth.LoginActivity
import com.denariidolor.util.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        binding.fabAddTransaction.setOnClickListener {
            navController.navigate(R.id.addTransactionFragment)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    delay(30_000)
                    if (sessionManager.isSessionTimedOut()) {
                        redirectToLogin()
                        break
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (sessionManager.isSessionTimedOut()) {
            redirectToLogin()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (sessionManager.isSessionTimedOut()) {
            redirectToLogin()
        } else {
            sessionManager.touch()
        }
    }

    private fun redirectToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}
