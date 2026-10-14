package com.upr.proxy;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Muestra el portal cautivo de la UPR en un WebView y autocompleta
 * usuario/contraseña.
 *
 * Flujo:
 *  1. Carga la URL del portal: upr-wifi.upr.edu.cu:8003
 *  2. Inyecta JavaScript para rellenar los campos con las credenciales
 *     guardadas en SecurePrefs.
 *  3. El usuario pulsa "Entrar".
 *  4. Cuando la URL sale del portal, se considera login exitoso.
 *  5. Espera 2s, cierra la activity y devuelve RESULT_OK.
 */
public class UprLoginActivity extends AppCompatActivity {

    private static final String TAG = "UprLogin";

    // URL del portal cautivo de la UPR
    private static final String PORTAL_URL =
            "https://upr-wifi.upr.edu.cu:8003/index.php?zone=upr_wifi&redirurl=" +
            "http%3A%2F%2Fintranet.upr.edu.cu%2F";

    private WebView webView;
    private ProgressBar progress;
    private TextView txtStatus;
    private String user;
    private String pass;
    private boolean loginDetected = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upr_login);

        SharedPreferences prefs = SecurePrefs.get(this);
        user = prefs.getString("user", "");
        pass = prefs.getString("password", "");

        webView = findViewById(R.id.webView);
        progress = findViewById(R.id.progressLogin);
        txtStatus = findViewById(R.id.txtLoginStatus);

        findViewById(R.id.btnCloseLogin).setOnClickListener(v -> finish());

        if (user.isEmpty() || pass.isEmpty()) {
            txtStatus.setText("⚠️ Faltan credenciales");
            Toast.makeText(this,
                    "Guarda tu usuario y contraseña en la pantalla principal primero",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Configurar WebView
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Cookies persistentes para mantener la sesión
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView v, String url, Bitmap icon) {
                progress.setVisibility(View.VISIBLE);
                L.i(TAG, "Cargando: " + url);
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                progress.setVisibility(View.GONE);
                L.i(TAG, "Página cargada: " + url);

                // Detectar login exitoso: la URL ya no es del portal
                if (!loginDetected
                        && !url.contains("upr-wifi.upr.edu.cu")
                        && !url.contains("index.php?zone")) {
                    loginDetected = true;
                    txtStatus.setText("✅ Sesión iniciada");
                    Toast.makeText(UprLoginActivity.this,
                            "✅ Sesión iniciada en la UPR",
                            Toast.LENGTH_SHORT).show();

                    webView.postDelayed(() -> {
                        setResult(RESULT_OK);
                        finish();
                    }, 2000);
                    return;
                }

                // Rellenar formulario si hay
                autofillCredentials();
            }

            @Override
            public void onReceivedSslError(WebView v, SslErrorHandler handler, SslError error) {
                String url = error.getUrl();
                // Aceptar solo certificados de dominios .upr.edu.cu
                if (url != null && url.contains("upr.edu.cu")) {
                    handler.proceed();
                    L.w(TAG, "SSL aceptado (UPR): " + url);
                } else {
                    handler.cancel();
                    L.e(TAG, "SSL rechazado: " + url);
                }
            }
        });

        txtStatus.setText("Iniciar sesión UPR");
        webView.loadUrl(PORTAL_URL);
    }

    private void autofillCredentials() {
        String u = jsEscape(user);
        String p = jsEscape(pass);

        String js =
            "(function() {" +
            "  function setVal(selectors, val) {" +
            "    for (var i = 0; i < selectors.length; i++) {" +
            "      var el = document.querySelector(selectors[i]);" +
            "      if (el && !el.value) {" +
            "        el.value = val;" +
            "        el.dispatchEvent(new Event('input', {bubbles: true}));" +
            "        el.dispatchEvent(new Event('change', {bubbles: true}));" +
            "        return true;" +
            "      }" +
            "    }" +
            "    return false;" +
            "  }" +
            "  var userSel = ['input[name=\"auth_user\"]','input[name=\"username\"]','input[name=\"user\"]','input[id=\"username\"]','input[type=\"text\"]'];" +
            "  var passSel = ['input[name=\"auth_pass\"]','input[name=\"password\"]','input[name=\"pass\"]','input[id=\"password\"]','input[type=\"password\"]'];" +
            "  var uOk = setVal(userSel, " + u + ");" +
            "  var pOk = setVal(passSel, " + p + ");" +
            "  console.log('[UprLogin] user=' + uOk + ' pass=' + pOk);" +
            "  return uOk && pOk;" +
            "})()";

        webView.evaluateJavascript(js, result -> L.i(TAG, "Autofill: " + result));
    }

    private static String jsEscape(String s) {
        if (s == null) return "\'\'";
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
