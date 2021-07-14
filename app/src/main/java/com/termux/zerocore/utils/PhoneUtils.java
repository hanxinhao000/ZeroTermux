package com.termux.zerocore.utils;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.zerocore.utermux_windows.qemu.data.MyContacts;

import java.util.ArrayList;

public class PhoneUtils {

        public static String getAllContacts(Context context) {
            ArrayList<MyContacts> contacts = new ArrayList<MyContacts>();

            Cursor cursor = context.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            while (cursor.moveToNext()) {
                //新建一个联系人实例
                MyContacts temp = new MyContacts();
                String contactId = cursor.getString(cursor
                    .getColumnIndex(ContactsContract.Contacts._ID));
                //获取联系人姓名
                String name = cursor.getString(cursor
                    .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                temp.name = name;

                //获取联系人电话号码
                Cursor phoneCursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactId, null, null);
                while (phoneCursor.moveToNext()) {
                    String phone = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    phone = phone.replace("-", "");
                    phone = phone.replace(" ", "");
                    temp.phone = phone;
                }

                //获取联系人备注信息
                Cursor noteCursor = context.getContentResolver().query(
                    ContactsContract.Data.CONTENT_URI,
                    new String[]{ContactsContract.Data._ID, ContactsContract.CommonDataKinds.Nickname.NAME},
                    ContactsContract.Data.CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + "='"
                        + ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE + "'",
                    new String[]{contactId}, null);
                if (noteCursor.moveToFirst()) {
                    do {
                        String note = noteCursor.getString(noteCursor
                            .getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME));
                        temp.note = note;
                        Log.i("note:", note);
                    } while (noteCursor.moveToNext());
                }
                contacts.add(temp);
                //记得要把cursor给close掉
                phoneCursor.close();
                noteCursor.close();
            }
            cursor.close();

            if(contacts.isEmpty()){

                return UUtils.getString(R.string.无联系人);

            }

            StringBuffer stringBuffer = new StringBuffer();

            for (int i = 0; i < contacts.size(); i++) {

                stringBuffer.append("\n");
                stringBuffer.append("\n");
                stringBuffer.append("\n");
                stringBuffer.append(UUtils.getString(R.string.序号));
                stringBuffer.append(":");
                stringBuffer.append(i);
                stringBuffer.append("\n");
                stringBuffer.append("\n");
                stringBuffer.append(UUtils.getString(R.string.姓名));
                stringBuffer.append(":");
                stringBuffer.append(contacts.get(i).name);
                stringBuffer.append("\n");
                stringBuffer.append(UUtils.getString(R.string.电话));
                stringBuffer.append(":");
                stringBuffer.append(contacts.get(i).phone);
                stringBuffer.append("\n");
                stringBuffer.append(UUtils.getString(R.string.备注));
                stringBuffer.append(":");
                stringBuffer.append(contacts.get(i).note);

                stringBuffer.append("\n");
                stringBuffer.append("\n");
                stringBuffer.append("\n");

            }


            return stringBuffer.toString();
        }

}
