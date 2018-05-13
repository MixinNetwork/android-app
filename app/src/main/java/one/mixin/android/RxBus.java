package one.mixin.android;

import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;

public class RxBus {
    private static volatile RxBus mInstance;

    public static RxBus getInstance() {
        RxBus rxBus2 = mInstance;
        if (mInstance == null) {
            synchronized (RxBus.class) {
                rxBus2 = mInstance;
                if (mInstance == null) {
                    rxBus2 = new RxBus();
                    mInstance = rxBus2;
                }
            }
        }
        return rxBus2;
    }

    private final FlowableProcessor<Object> mBus;

    private RxBus() {
        mBus = PublishProcessor.create().toSerialized();
    }

    public void post(Object obj) {
        mBus.onNext(obj);
    }

    public <T> Flowable<T> toFlowable(Class<T> tClass) {
        return mBus.ofType(tClass);
    }

    @SuppressWarnings("unused")
    public Flowable<Object> toFlowable() {
        return mBus;
    }

    @SuppressWarnings("unused")
    public boolean hasSubscribers() {
        return mBus.hasSubscribers();
    }

}