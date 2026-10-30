package com.dvopenlabs.freedom.companions;

import android.app.*;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.concurrent.*;

/** Shared optional-panel shell; stable native focus, bounded workers and no work after pause. */
public abstract class PanelActivity extends Activity {
    LinearLayout page;volatile int generation;volatile boolean visible;ThreadPoolExecutor worker;final Handler main=new Handler(Looper.getMainLooper());
    int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextColor(0xfff3f6fc);t.setTextSize(size);t.setTypeface(getResources().getFont(R.font.outfit));t.setFontVariationSettings(size>=26?"'wght' 550":"'wght' 400");t.setPadding(0,dp(4),0,dp(10));return t;}
    void page(String title,String intro,boolean secret){generation++;if(worker!=null)worker.getQueue().clear();if(secret)getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(40),dp(24),dp(40),dp(24));page.setBackgroundColor(0xff101923);page.addView(text(title,28));page.addView(text(intro,17));ScrollView s=new ScrollView(this);s.addView(page);setContentView(s);}
    TextView button(String label,Runnable action){TextView t=text(label,18);t.setPadding(dp(16),dp(12),dp(16),dp(12));t.setMinHeight(dp(48));t.setFocusable(true);t.setClickable(true);t.setBackgroundResource(R.drawable.freedom_action);t.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_focused},new int[0]},new int[]{0xff182334,0xfff3f6fc}));t.setOnClickListener(v->action.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);page.addView(t,p);if(page.getChildCount()==3)t.post(()->{if(t.isInTouchMode())t.requestFocusFromTouch();else t.requestFocus();});return t;}
    EditText field(String label,String value,boolean secret){page.addView(text(label,16));EditText e=new EditText(this);e.setTextColor(-1);e.setText(value);e.setSingleLine(true);e.setTextSize(18);e.setInputType(InputType.TYPE_CLASS_TEXT|(secret?InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_TEXT_VARIATION_NORMAL));e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);page.addView(e,new LinearLayout.LayoutParams(-1,dp(56)));return e;}
    interface Job<T>{T run()throws Exception;}
    <T> void load(Job<T> job,java.util.function.Consumer<T> success,java.util.function.Consumer<Exception> failed){int token=generation;if(!visible||worker==null||worker.isShutdown())return;try{worker.execute(()->{if(!visible||token!=generation)return;try{T result=job.run();main.post(()->{if(visible&&token==generation)success.accept(result);});}catch(Exception bad){main.post(()->{if(visible&&token==generation)failed.accept(bad);});}});}catch(RejectedExecutionException busy){if(visible&&token==generation)failed.accept(busy);}}
    @Override public void onResume(){super.onResume();visible=true;worker=new ThreadPoolExecutor(2,2,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(24));home();}
    abstract void home();
    @Override public void onPause(){visible=false;generation++;main.removeCallbacksAndMessages(null);if(worker!=null)worker.shutdownNow();super.onPause();}
}
