/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: aidl/ILicensingService.aidl
 */
package com.jasonfoglia.universalrobotcontroller.licensing;

import android.os.IBinder;

public interface ILicensingService extends android.os.IInterface {
    /**
     * Local-side IPC implementation stub class.
     */
    public static abstract class Stub extends android.os.Binder implements com.jasonfoglia.universalrobotcontroller.licensing.ILicensingService {
        private static final String DESCRIPTOR = "com.android.vending.com.jasonfoglia.universalrobotcontroller.licensing.ILicensingService";

        /**
         * Construct the stub at attach it to the interface.
         */
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        /**
         * Cast an IBinder object into an ILicensingService interface,
         * generating a proxy if needed.
         */
        public static com.jasonfoglia.universalrobotcontroller.licensing.ILicensingService asInterface(IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            android.os.IInterface iin = (android.os.IInterface) obj.queryLocalInterface(DESCRIPTOR);
            if (((iin != null) && (iin instanceof com.jasonfoglia.universalrobotcontroller.licensing.ILicensingService))) {
                return ((com.jasonfoglia.universalrobotcontroller.licensing.ILicensingService) iin);
            }
            return new com.jasonfoglia.universalrobotcontroller.licensing.ILicensingService.Stub.Proxy(obj);
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                case TRANSACTION_checkLicense: {
                    data.enforceInterface(DESCRIPTOR);
                    long _arg0;
                    _arg0 = data.readLong();
                    String _arg1;
                    _arg1 = data.readString();
                    com.jasonfoglia.universalrobotcontroller.licensing.ILicenseResultListener _arg2;
                    _arg2 = com.jasonfoglia.universalrobotcontroller.licensing.ILicenseResultListener.Stub.asInterface(data.readStrongBinder());
                    this.checkLicense(_arg0, _arg1, _arg2);
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements com.jasonfoglia.universalrobotcontroller.licensing.ILicensingService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            public IBinder asBinder() {
                return mRemote;
            }

            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            public void checkLicense(long nonce, String packageName, com.jasonfoglia.universalrobotcontroller.licensing.ILicenseResultListener listener) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeLong(nonce);
                    _data.writeString(packageName);
                    _data.writeStrongBinder((((listener != null)) ? (listener.asBinder()) : (null)));
                    mRemote.transact(Stub.TRANSACTION_checkLicense, _data, null, IBinder.FLAG_ONEWAY);
                } finally {
                    _data.recycle();
                }
            }
        }

        static final int TRANSACTION_checkLicense = (IBinder.FIRST_CALL_TRANSACTION + 0);
    }

    public void checkLicense(long nonce, String packageName, com.jasonfoglia.universalrobotcontroller.licensing.ILicenseResultListener listener) throws android.os.RemoteException;
}
