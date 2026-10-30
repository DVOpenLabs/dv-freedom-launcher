package com.dvopenlabs.freedom.remote;
import android.accessibilityservice.AccessibilityService;
import android.os.*;
import android.view.View;
import android.view.accessibility.*;

/** Events are ignored. Focus is inspected only during a paired, user-sent action. */
public final class RemoteAccess extends AccessibilityService {
    static RemoteAccess current;
    @Override public void onServiceConnected(){current=this;}
    @Override public void onAccessibilityEvent(AccessibilityEvent e){}
    @Override public void onInterrupt(){}
    @Override public void onDestroy(){if(current==this)current=null;super.onDestroy();}
    boolean act(String key,String text){
        if(key.equals("home"))return performGlobalAction(GLOBAL_ACTION_HOME);
        if(key.equals("back"))return performGlobalAction(GLOBAL_ACTION_BACK);
        int action=key.equals("up")?GLOBAL_ACTION_DPAD_UP:key.equals("down")?GLOBAL_ACTION_DPAD_DOWN:key.equals("left")?GLOBAL_ACTION_DPAD_LEFT:key.equals("right")?GLOBAL_ACTION_DPAD_RIGHT:key.equals("ok")?GLOBAL_ACTION_DPAD_CENTER:0;
        if(Build.VERSION.SDK_INT>=33&&action!=0)return performGlobalAction(action);
        AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return false;
        try{AccessibilityNodeInfo node=root.findFocus(key.equals("text")||key.equals("enter")?AccessibilityNodeInfo.FOCUS_INPUT:AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);if(node==null)node=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);if(node==null)return false;
            try{if(key.equals("text")){if(!node.isEditable()||node.isPassword()||text==null||text.length()>1000)return false;Bundle b=new Bundle();b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b);}
                if(key.equals("enter"))return Build.VERSION.SDK_INT>=30&&!node.isPassword()&&node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId());
                if(key.equals("ok"))return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                int direction=key.equals("up")?View.FOCUS_UP:key.equals("down")?View.FOCUS_DOWN:key.equals("left")?View.FOCUS_LEFT:key.equals("right")?View.FOCUS_RIGHT:0;if(direction==0)return false;AccessibilityNodeInfo next=node.focusSearch(direction);if(next==null)return false;try{return next.performAction(AccessibilityNodeInfo.ACTION_FOCUS);}finally{next.recycle();}
            }finally{node.recycle();}
        }finally{root.recycle();}
    }
}
