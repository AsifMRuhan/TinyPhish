package com.example.tinyphish

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var detector: PhishingDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Android 15+: draw behind the status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        // Use white icons on the dark-blue status bar
        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).isAppearanceLightStatusBars = false

        // Handle the status-bar inset without moving content underneath it
        val mainScrollView =
            findViewById<ScrollView>(R.id.mainScrollView)

        val originalTop = mainScrollView.paddingTop

        ViewCompat.setOnApplyWindowInsetsListener(mainScrollView) { view, insets ->
            val topInset = insets.getInsets(
                WindowInsetsCompat.Type.statusBars()
            ).top

            view.setPadding(
                view.paddingLeft,
                originalTop + topInset,
                view.paddingRight,
                view.paddingBottom
            )

            insets
        }

        ViewCompat.requestApplyInsets(mainScrollView)

        // Load the decision-tree model
        try {
            val text = assets.open("phishing_tree_final_cw3.json")
                .bufferedReader()
                .use { it.readText() }

            detector = PhishingDetector(JSONObject(text))
            Log.d("TinyPhish", "Model loaded successfully")

        } catch (e: Exception) {
            Log.e("TinyPhish", "Model loading failed", e)
            return
        }

        // Day 5: Test features and predictions
        val testUrls = listOf(
            "https://www.google.com",
            "http://192.168.1.1/login@verify",
            "192.168.1.1/login",
            "HTTPS://Example.com/path",
            "example.com/path",
            "https://[2001:db8::1]/login"
        )

        for (url in testUrls) {
            try {
                val features = detector.testFeatures(url)
                val prediction = detector.predict(url)
                val label = if (prediction == 1) "Phishing" else "Benign"

                Log.d(
                    "TinyPhishTest",
                    "URL: $url | Features: ${features.contentToString()} | " +
                            "Prediction: $prediction - $label"
                )

            } catch (e: Exception) {
                Log.e("TinyPhishTest", "Test failed: $url", e)
            }
        }

        // Connect UI elements
        val urlInput = findViewById<EditText>(R.id.urlInput)
        val checkButton = findViewById<Button>(R.id.checkButton)
        val resultCard = findViewById<LinearLayout>(R.id.resultCard)
        val resultTitle = findViewById<TextView>(R.id.resultTitle)
        val resultMessage = findViewById<TextView>(R.id.resultMessage)

        // Check the entered URL
        checkButton.setOnClickListener {

            // Hide keyboard and clear input focus
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager

            imm.hideSoftInputFromWindow(urlInput.windowToken, 0)
            urlInput.clearFocus()

            val url = urlInput.text.toString().trim()

            // Show feedback for the new check
            resultTitle.text = "Checking URL..."
            resultMessage.text =
                "Please wait while TinyPhish analyzes the URL."

            setResultCardColor(resultCard, "#FFFFFF", "#E0E7F0")
            resultTitle.setTextColor(Color.parseColor("#173B68"))

            if (url.isEmpty()) {
                resultTitle.text = "Enter a URL"
                resultMessage.text = "Please enter a URL to check."

                setResultCardColor(resultCard, "#FFFFFF", "#E0E7F0")
                resultTitle.setTextColor(Color.parseColor("#173B68"))

            } else {

                // Add HTTPS for validation when no scheme is provided
                val urlToValidate =
                    if (
                        url.startsWith("http://", ignoreCase = true) ||
                        url.startsWith("https://", ignoreCase = true)
                    ) {
                        url
                    } else {
                        "https://$url"
                    }

                val parsedUri = Uri.parse(urlToValidate)
                val host = parsedUri.host

                // Accept only HTTP or HTTPS
                val validScheme =
                    parsedUri.scheme.equals("http", ignoreCase = true) ||
                            parsedUri.scheme.equals("https", ignoreCase = true)

                // Validate hostname
                val validHost = !host.isNullOrBlank() &&
                        (
                                host.matches(
                                    Regex(
                                        "(?i)^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?" +
                                                "(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+$"
                                    )
                                ) || host.contains(":")
                                )

                val validUrl = validScheme &&
                        validHost &&
                        !url.any { it.isWhitespace() }

                if (!validUrl) {
                    resultTitle.text = "Invalid URL"
                    resultMessage.text =
                        "Enter a valid web address, such as example.com or https://example.com."

                    setResultCardColor(resultCard, "#FFF4E5", "#E8C58A")
                    resultTitle.setTextColor(Color.parseColor("#8A4B08"))

                } else {
                    try {
                        // Run the existing model
                        val prediction = detector.predict(url)

                        // Get URL characteristics
                        val evidence = detector.getUrlEvidence(url)

                        // Display model prediction
                        if (prediction == 1) {
                            resultTitle.text = "⚠ PHISHING DETECTED"

                            setResultCardColor(
                                resultCard,
                                "#FDECEC",
                                "#E7A3A3"
                            )

                            resultTitle.setTextColor(
                                Color.parseColor("#B42318")
                            )

                        } else {
                            resultTitle.text = "✓ NO PHISHING DETECTED"

                            setResultCardColor(
                                resultCard,
                                "#E8F7EE",
                                "#9BD5B1"
                            )

                            resultTitle.setTextColor(
                                Color.parseColor("#16734D")
                            )
                        }

                        // Display evidence for both results
                        resultMessage.text = buildString {
                            appendLine(
                                if (prediction == 1) {
                                    "The model detected phishing-like patterns."
                                } else {
                                    "The model did not classify this URL as phishing."
                                }
                            )

                            appendLine()
                            appendLine("URL characteristics:")

                            evidence.forEach { item ->
                                appendLine("• $item")
                            }

                            appendLine()
                            append(
                                "These characteristics are clues, not proof. " +
                                        "A benign prediction does not guarantee safety."
                            )
                        }

                    } catch (e: Exception) {
                        Log.e("TinyPhish", "Prediction failed", e)

                        resultTitle.text = "Unable to Check"
                        resultMessage.text =
                            "An error occurred while checking this URL."

                        setResultCardColor(
                            resultCard,
                            "#FFF4E5",
                            "#E8C58A"
                        )

                        resultTitle.setTextColor(
                            Color.parseColor("#8A4B08")
                        )
                    }
                }
            }

            // Day 6, Fix 5: Scroll to the result
            resultCard.post {
                mainScrollView.smoothScrollTo(0, resultCard.bottom)
            }
        }

        // Make the keyboard's Go/Done/Search key check the URL
        urlInput.setOnEditorActionListener { _, actionId, _ ->
            if (
                actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_SEARCH
            ) {
                checkButton.performClick()
                true
            } else {
                false
            }
        }
    }

    // Change the result card's background and border colors
    private fun setResultCardColor(
        card: LinearLayout,
        backgroundColor: String,
        borderColor: String
    ) {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = 18f * resources.displayMetrics.density
        shape.setColor(Color.parseColor(backgroundColor))

        shape.setStroke(
            (1 * resources.displayMetrics.density).toInt(),
            Color.parseColor(borderColor)
        )

        card.background = shape
    }
}
