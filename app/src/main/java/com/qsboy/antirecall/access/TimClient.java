/*
 * Copyright © 2016 - 2018 by GitHub.com/JasonQS
 * anti-recall.qsboy.com
 * All Rights Reserved
 */

package com.qsboy.antirecall.access;

import android.content.Context;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class TimClient extends Client {

    final String IdTitle = "com.tencent.tim:id/title";
    final String IdChatGroupView = "com.tencent.tim:id/listView1";
    final String IdHeadIcon = "com.tencent.tim:id/chat_item_head_icon";
    final String IdChatItem = "com.tencent.tim:id/chat_item_content_layout";
    final String IdNickName = "com.tencent.tim:id/chat_item_nick_name";
    final String IdOtherMsg = "com.tencent.tim:id/msgbox";
    final String IdGrayBar = "com.tencent.tim:id/graybar";
    final String IdInput = "com.tencent.tim:id/input";
    final String IdSend = "com.tencent.tim:id/fun_btn";
    String TAG = "Tim";

    public TimClient(Context context) {
        super(context);
        isWX = false;
        client = "Tim";
    }

    /**
     * 好友：
     * 姓名       1    android.widget.TextView
     * 消息       5-[]-last android.widget.TextView   focusable
     * 文本框     7-0  android.widget.EditText
     * 发送按钮   7-1  android.widget.Button
     * <p>
     * 群：
     * 群名       1
     * 消息       4/5-
     * <p>
     * 群里的临时会话：
     * 姓名       1-0
     * 消息       6-[]-last
     * 文本框     8-0
     * 发送按钮   8-1
     */
    protected boolean init(AccessibilityNodeInfo root) {
        //16 是其他界面
        //14 是没有聊过天
        //12 是发送完消息
        if (root.getChildCount() < 12) {
            Log.v(TAG, "init: root.childCount: " + root.getChildCount());
            return false;
        }

        List<AccessibilityNodeInfo> titleList;
        List<AccessibilityNodeInfo> inputList;
        List<AccessibilityNodeInfo> sendList;

        titleList = root.findAccessibilityNodeInfosByViewId(IdTitle);
        inputList = root.findAccessibilityNodeInfosByViewId(IdInput);
        sendList = root.findAccessibilityNodeInfosByViewId(IdSend);

        if (titleList.size() == 0) {
            Log.d(TAG, "init: title is null, return");
            return false;
        }
        if (inputList.size() == 0) {
            Log.d(TAG, "init: input is null, return");
            return false;
        }
        if (sendList.size() == 0) {
            Log.d(TAG, "init: send button is null, return");
            return false;
        }
        titleNode = titleList.get(0);
        inputNode = inputList.get(0);
        sendBtnNode = sendList.get(0);
        if (titleNode.getText() == null) {
            Log.d(TAG, "init: name is null，return");
            return false;
        }
        title = titleNode.getText() + "";
        isOtherMsg = false;

        for (int i = 4; i < 7; i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            String name = child.getViewIdResourceName();
            if (name == null)
                continue;
            switch (name) {
                case IdChatGroupView:
                    chatGroupViewNode = child;
                    break;
                case IdOtherMsg:
                    otherMsgNode = child;
                    isOtherMsg = true;
                    break;
            }
        }

        if (chatGroupViewNode == null) {
            Log.i(TAG, "init: chatGroupViewNode is null, return");
            return false;
        }

        return true;
    }

    protected void parser(AccessibilityNodeInfo group) {
        int headIconPos = 0;
        int messagePos = 0;
        subName = "";
        message = "";
        isRecalledMsg = false;
        int childCount = group.getChildCount();

        for (int j = 0; j < childCount; j++) {
            AccessibilityNodeInfo child = group.getChild(j);
            if (child == null) {
                Log.d(TAG, "init: child is null, continue");
                continue;
            }
            String nodeId = child.getViewIdResourceName();
            if (nodeId == null) {
                Log.d(TAG, "init: node ID is null, continue");
                continue;
            }
            switch (nodeId) {
                case IdHeadIcon:
                    //头像图标
                    headIconPos = j;
                    break;
                case IdChatItem:
                    switch (child.getClassName() + "") {
                        // TODO: 16/04/2018 签到
                        case "android.widget.RelativeLayout":
                            if (child.getChildCount() != 0) {
                                if (child.getContentDescription() != null) {
                                    redPegNode = child;
                                    message = "红包";
                                    // TODO: 红包或者是分享
                                    Log.v(TAG, "content_layout: 红包");
                                }
                            } else {
                                message = "[图片]";
                                Log.v(TAG, "content_layout: 图片");
                            }
                            break;
                        case "android.widget.LinearLayout": {
                            if (child.getChildCount() == 2) {
                                AccessibilityNodeInfo child1 = child.getChild(0);
                                AccessibilityNodeInfo child2 = child.getChild(1);
                                if (child1 != null && "android.widget.TextView".contentEquals(child1.getClassName())) {
                                    if (child2 != null && "android.widget.TextView".contentEquals(child2.getClassName())) {
                                        message = child2.getText() + "";
                                    }
                                }
                            }
                            Log.v(TAG, "content_layout: 回复消息");
                        }
                        break;
                        case "android.widget.TextView": {
                            message = child.getText() + "";
                            Log.v(TAG, "content_layout: 普通文本");
                        }
                        break;
                    }
                    messagePos = j;
                    break;
                case IdNickName:
                    //群聊头像上面的群昵称最后有一个冒号
                    subName = child.getText() + "";
                    subName = subName.substring(0, subName.length() - 1);
                    break;
                case IdGrayBar:
                    // 撤回消息或者是有人加入
                    // 撤回消息的是不可点击的
                    // 接收文件和撤回消息一样
                    if (!child.isClickable() && !child.isFocusable()) {
                        message = child.getText() + "";
                        int indexOfRecall = message.indexOf(RECALL);
                        if (indexOfRecall >= 0) {
                            isRecalledMsg = true;
                            subName = message.substring(0, indexOfRecall);
                            message = message.substring(indexOfRecall);
                            if ("对方".equals(subName))
                                subName = title;
                            else if ("你".equals(subName))
                                subName = "我";
                        }
                    }

            }
        }
        if (messagePos < headIconPos)
            // 消息在头像左边
            subName = "我";
        else if ("".equals(subName))
            // 两人聊天时 没有subName
            subName = title;
        Log.d(TAG, "parser: " + title + " - " + subName + " : " + message);
    }

}
