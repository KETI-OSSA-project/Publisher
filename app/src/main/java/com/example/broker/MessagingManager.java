package com.example.broker;

import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import java.util.Objects;

public class MessagingManager implements AutoCloseable {
    private final BoundServiceHelper<IMessagingService> serviceHelper;

    private MessagingManager(@NonNull Context context) {
        Objects.requireNonNull(context);
        // serviceHelper = BoundServiceHelper 객체
        serviceHelper = BoundServiceHelper.newInstanceWithAction(context,
                Constants.MESSAGING_SERVICE_ACTION,
                IMessagingService.Stub::asInterface);
    }
    @NonNull
    public static MessagingManager getMessagingManager(Context context) {
        return new MessagingManager(context);
    }

    public void setMessagingServiceConnectionListener(
            @Nullable MessagingServiceConnectionListener listener, @Nullable Handler handler) {
        serviceHelper.setExternalConnectionListener(buildConnectionListener(listener), handler);
    }

    @Nullable
    private BoundServiceHelper.ConnectionListener buildConnectionListener(
            @Nullable MessagingServiceConnectionListener listener) {
        if (listener == null) return null;
        return new BoundServiceHelper.ConnectionListener() {
            @Override
            public void onConnected() { listener.onConnected(); }
            @Override
            public void onDisconnected() { listener.onDisconnected(); }
        };
    }

    @Override
    public void close() {
        serviceHelper.close();
    }

    public boolean publish(@NonNull com.example.broker.Message message) {
        Objects.requireNonNull(message);
        IMessagingService service = serviceHelper.get();
        if (service == null) {
            throw new RuntimeException("Android Broker가 아직 바인딩되지 않았습니다. 연결을 기다려주세요.");
        }
        try {
            service.publish(message);
            return true;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
    public interface MessagingServiceConnectionListener {
        void onConnected();
        void onDisconnected();
    }

    // 바인딩 주소 정리한 곳
    public static class Constants {
        public static final String MESSAGE_BROKER_PACKAGE = "com.example.broker";
        public static final String MESSAGING_SERVICE = "com.example.broker.MessagingService";
        public static final String MESSAGING_SERVICE_ACTION = "com.example.broker.MessagingService";
    }

    // BoundServiceHelper 통합
    public static class BoundServiceHelper<T> {
        private static final String TAG = "BoundServiceHelper";
        public interface ConnectionListener {
            void onConnected();
            void onDisconnected();
        }
        public interface BinderConverter<T> {
            T convert(android.os.IBinder binder);
        }
        private final Context context;
        private final BinderConverter<T> binderConverter;
        private final java.util.concurrent.atomic.AtomicReference<T> serviceRef = new java.util.concurrent.atomic.AtomicReference<>();
        private boolean bound = false;
        private ConnectionListener externalListener;
        private Handler externalHandler;
        private final android.content.ServiceConnection connection = new android.content.ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, android.os.IBinder binder) {
                T service = binderConverter.convert(binder);
                serviceRef.set(service);
                bound = true;
                notifyExternalConnected();
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                bound = false;
                serviceRef.set(null);
                notifyExternalDisconnected();
            }
        };
        private BoundServiceHelper(@NonNull Context context, @Nullable String action, @NonNull BinderConverter<T> binderConverter) {
            this.context = context.getApplicationContext();
            this.binderConverter = binderConverter;
            bindService();
        }
        public static <T> BoundServiceHelper<T> newInstanceWithAction(@NonNull Context context, @NonNull String action, @NonNull BinderConverter<T> binderConverter) {
            return new BoundServiceHelper<>(context, action, binderConverter);
        }

        public void bindService() {
            try {
                android.content.Intent intent = new android.content.Intent();
                intent.setComponent(new ComponentName(Constants.MESSAGE_BROKER_PACKAGE, Constants.MESSAGING_SERVICE));
                boolean success = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
                if (!success) {
                    Log.w(TAG, "바인딩 실패");
                    notifyExternalDisconnected();
                }
            } catch (Exception e) {
                Log.e(TAG, "바인딩 시도 중 예외", e);
                notifyExternalDisconnected();
            }
        }
        public void setExternalConnectionListener(ConnectionListener listener, Handler handler) {
            this.externalListener = listener;
            this.externalHandler = handler;
        }
        private void notifyExternalConnected() {
            if (externalListener != null) {
                if (externalHandler != null) {
                    externalHandler.post(externalListener::onConnected);
                } else {
                    externalListener.onConnected();
                }
            }
        }
        private void notifyExternalDisconnected() {
            if (externalListener != null) {
                if (externalHandler != null) {
                    externalHandler.post(externalListener::onDisconnected);
                } else {
                    externalListener.onDisconnected();
                }
            }
        }
        public T get() {
            return serviceRef.get();
        }
        public void close() {
            if (bound) {
                context.unbindService(connection);
                bound = false;
            }
        }
    }
}
