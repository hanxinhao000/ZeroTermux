package org.las2mile.scrcpy.model;

/**
 * Created by Alexandr Golovach on 27.06.16.
 */
public class MediaPacket {

    public Type type;

    public enum Type {

        VIDEO((byte) 1), AUDIO((byte) 0);

        private byte type;

        Type(byte type) {
            this.type = type;
        }

        public static Type getType(byte value) {
            for (Type type : Type.values()) {
                if (type.getType() == value) {
                    return type;
                }
            }

            return null;
        }

        public byte getType() {
            return type;
        }
    }
}
