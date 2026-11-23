package in.zero.irctcsafeassistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.content.SharedPreferences;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.InputType;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView web;
    private EditText passengerName, passengerAge;
    private String gender = "Male";
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("passenger_profile", MODE_PRIVATE);
        gender = prefs.getString("gender", "Male");
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("IRCTC  •  SAFE ASSISTANT");
        title.setTextColor(Color.WHITE); title.setTextSize(17); title.setPadding(16,16,8,16);
        title.setBackgroundColor(Color.rgb(20,55,105));
        root.addView(title, new LinearLayout.LayoutParams(-1,-2));

        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(6,6,6,6);
        Button back = button("Back"), reload = button("Reload"), details = button("Passenger details"), fill = button("Fill now");
        actions.addView(back, weight()); actions.addView(reload, weight());
        actions.addView(details, weight()); actions.addView(fill, weight());
        root.addView(actions);

        web = new WebView(this);
        web.setBackgroundColor(Color.WHITE);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setLoadsImagesAutomatically(true);
        web.getSettings().setSupportZoom(true);
        web.getSettings().setBuiltInZoomControls(true);
        web.getSettings().setDisplayZoomControls(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String host = request.getUrl().getHost();
                if (host != null && (host.equals("irctc.co.in") || host.endsWith(".irctc.co.in"))) return false;
                Toast.makeText(MainActivity.this, "External navigation blocked for safety", Toast.LENGTH_SHORT).show();
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                installAutofillWatcher();
            }
        });
        root.addView(web, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);

        back.setOnClickListener(v -> { if(web.canGoBack()) web.goBack(); });
        reload.setOnClickListener(v -> web.reload());
        details.setOnClickListener(v -> showPassengerDialog());
        fill.setOnClickListener(v -> fillPassenger());
        if (state == null) web.loadUrl("https://www.irctc.co.in/nget/train-search");
        else web.restoreState(state);
    }

    private Button button(String label) {
        Button b = new Button(this); b.setText(label); b.setTextSize(10); return b;
    }
    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0,-2,1); }

    private void showPassengerDialog() {
        LinearLayout box = new LinearLayout(this); box.setPadding(24,8,24,0); box.setOrientation(LinearLayout.VERTICAL);
        passengerName = new EditText(this); passengerName.setHint("Passenger full name");
        passengerName.setText(prefs.getString("name", ""));
        passengerAge = new EditText(this); passengerAge.setHint("Age"); passengerAge.setInputType(InputType.TYPE_CLASS_NUMBER);
        passengerAge.setText(prefs.getString("age", ""));
        box.addView(passengerName); box.addView(passengerAge);
        String[] genders = {"Male","Female","Transgender"};
        int selected = gender.equals("Female") ? 1 : gender.equals("Transgender") ? 2 : 0;
        new AlertDialog.Builder(this).setTitle("Passenger details")
            .setView(box).setSingleChoiceItems(genders,selected,(d,w)->gender=genders[w])
            .setPositiveButton("Save & enable autofill", (d,w)->{
                String name=passengerName.getText().toString().trim(), age=passengerAge.getText().toString().trim();
                if(name.isEmpty() || age.isEmpty()) { Toast.makeText(this,"Enter passenger name and age",Toast.LENGTH_SHORT).show(); return; }
                prefs.edit().putString("name",name).putString("age",age).putString("gender",gender).apply();
                Toast.makeText(this,"Saved on this device. Autofill will run on IRCTC pages when matching fields appear.",Toast.LENGTH_LONG).show();
                installAutofillWatcher();
            })
            .setNegativeButton("Cancel",null).show();
    }

    private String autofillScript() {
        String name=JSONObject.quote(prefs.getString("name",""));
        String age=JSONObject.quote(prefs.getString("age",""));
        String sex=JSONObject.quote(prefs.getString("gender","Male"));
        return "(function(){const vals=[{v:"+name+",keys:['passenger name','name']},{v:"+age+",keys:['age']},{v:"+sex+",keys:['gender','sex']}];" +
          "function scan(){let count=0;const fields=[...document.querySelectorAll('input,select,textarea')];" +
          "for(const f of fields){if(!f.offsetParent||f.disabled||f.readOnly)continue;" +
          "const type=(f.type||'').toLowerCase();if(['password','email','tel','hidden','checkbox','radio','file'].includes(type))continue;" +
          "const hint=((f.getAttribute('aria-label')||'')+' '+(f.placeholder||'')+' '+(f.name||'')+' '+(f.id||'')+' '+(f.labels?[...f.labels].map(x=>x.innerText).join(' '):'')).toLowerCase();" +
          "if(/captcha|otp|verification|payment|upi|card|account|username|login/.test(hint))continue;" +
          "for(const item of vals){if(!item.v||!item.keys.some(k=>hint.includes(k)))continue;" +
          "if(f.dataset.safeAssistantFilled==='1')break;" +
          "if(f.tagName==='SELECT'){const o=[...f.options].find(x=>x.text.toLowerCase().includes(item.v.toLowerCase())||x.value.toLowerCase()===item.v.toLowerCase());if(!o)break;f.value=o.value;}" +
          "else {const setter=Object.getOwnPropertyDescriptor(Object.getPrototypeOf(f),'value')?.set;if(setter)setter.call(f,item.v);else f.value=item.v;}" +
          "f.dataset.safeAssistantFilled='1';f.dispatchEvent(new Event('input',{bubbles:true}));f.dispatchEvent(new Event('change',{bubbles:true}));count++;break;}}" +
          "}return count;} scan(); if(!window.__safeAssistantObserver){window.__safeAssistantObserver=new MutationObserver(()=>scan());window.__safeAssistantObserver.observe(document.documentElement,{childList:true,subtree:true});}" +
          "if(!window.__safeAssistantHistory){window.__safeAssistantHistory=true;['pushState','replaceState'].forEach(k=>{const old=history[k];history[k]=function(){const r=old.apply(this,arguments);setTimeout(scan,300);return r;};});window.addEventListener('popstate',()=>setTimeout(scan,300));}" +
          "return 'watching';})()";
    }

    private void installAutofillWatcher() {
        if (web == null || prefs.getString("name","").isEmpty() || prefs.getString("age","").isEmpty()) return;
        web.evaluateJavascript(autofillScript(), null);
    }

    private void fillPassenger() {
        if (prefs.getString("name","").isEmpty() || prefs.getString("age","").isEmpty()) {
            showPassengerDialog(); return;
        }
        web.evaluateJavascript(autofillScript(), value ->
            Toast.makeText(this,"Autofill checked this page. Review all details before continuing.",Toast.LENGTH_LONG).show());
    }

    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }
    @Override public void onBackPressed() { if(web != null && web.canGoBack()) web.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() { if(web != null) web.destroy(); super.onDestroy(); }
}
