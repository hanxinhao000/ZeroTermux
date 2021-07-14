package com.termux.zerocore.utermux_windows.qemu.data;

public class MyContacts {

    public String name;
    public String phone;
    public String note;

    @Override
    public String toString() {
        return "MyContacts{" +
            "name='" + name + '\'' +
            ", phone='" + phone + '\'' +
            ", note='" + note + '\'' +
            '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
