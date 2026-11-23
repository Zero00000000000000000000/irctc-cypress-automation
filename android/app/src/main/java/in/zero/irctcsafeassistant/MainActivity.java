package in.zero.irctcsafeassistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
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

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
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
        Button back = button("Back"), reload = button("Reload"), details = button("Passenger details"), fill = button("Fill details");
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
        passengerAge = new EditText(this); passengerAge.setHint("Age"); passengerAge.setInputType(InputType.TYPE_CLASS_NUMBER);
        box.addView(passengerName); box.addView(passengerAge);
        new AlertDialog.Builder(this).setTitle("Passenger details (temporary)")
            .setView(box).setSingleChoiceItems(new String[]{"Male","Female","Transgender"},0,(d,w)->gender=new String[]{"Male","Female","Transgender"}[w])
            .setPositiveButton("Save on this screen", (d,w)->Toast.makeText(this,"Details ready. Tap Fill details on the booking form.",Toast.LENGTH_LONG).show())
            .setNegativeButton("Cancel",null).show();
    }

    private void fillPassenger() {
        if (passengerName == null || passengerName.getText().toString().trim().isEmpty()
                || passengerAge == null || passengerAge.getText().toString().trim().isEmpty()) {
            Toast.makeText(this,"Enter passenger name and age first",Toast.LENGTH_SHORT).show(); return;
        }
        try {
            String name = JSONObject.quote(passengerName.getText().toString().trim());
            String age = JSONObject.quote(passengerAge.getText().toString().trim());
            String sex = JSONObject.quote(gender);
            String js = "(function(){const vals=[{v:"+name+",keys:['passenger name','name']},{v:"+age+",keys:['age']},{v:"+sex+",keys:['gender','sex']}];" +
                "let changed=0; const fields=[...document.querySelectorAll('input,select,textarea')].filter(e=>e.offsetParent!==null && !e.disabled && !e.readOnly);" +
                "for(const f of fields){const hint=((f.getAttribute('aria-label')||'')+' '+(f.placeholder||'')+' '+(f.name||'')+' '+(f.id||'')+' '+(f.labels?[...f.labels].map(x=>x.innerText).join(' '):'')).toLowerCase();" +
                "for(const item of vals){if(item.keys.some(k=>hint.includes(k))){if(f.tagName==='SELECT'){const o=[...f.options].find(x=>x.text.toLowerCase().includes(item.v.toLowerCase())||x.value.toLowerCase()===item.v.toLowerCase());if(o)f.value=o.value;}else f.value=item.v;" +
                "f.dispatchEvent(new Event('input',{bubbles:true}));f.dispatchEvent(new Event('change',{bubbles:true}));changed++;break;}}}" +
                "return changed;})()";
            web.evaluateJavascript(js, value -> Toast.makeText(this,"Filled " + value + " matching field(s). Review every field before continuing.",Toast.LENGTH_LONG).show());
        } catch(Exception e) { Toast.makeText(this,"Could not fill fields",Toast.LENGTH_SHORT).show(); }
    }

    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }
    @Override public void onBackPressed() { if(web != null && web.canGoBack()) web.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() { if(web != null) web.destroy(); super.onDestroy(); }
}
