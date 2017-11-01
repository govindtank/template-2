package com.smart.housekeeper.repository.remote

import com.smart.housekeeper.repository.remote.core.HKOkHttpManager
import com.smart.housekeeper.repository.remote.core.HKResponseModel
import com.smart.housekeeper.repository.remote.exception.HKRetrofitException
import com.smart.housekeeper.repository.remote.exception.HKRetrofitServerException
import com.smart.library.base.HKActivityLifecycleCallbacks
import com.smart.library.base.HKBaseApplication
import com.smart.library.util.HKToastUtil
import com.smart.library.util.cache.HKCacheFileManager
import com.smart.library.util.rx.RxBus
import com.smart.library.widget.debug.HKTestingFragment
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.Serializable


internal object HKApiManager {

    fun init() {
        if (HKBaseApplication.DEBUG) {
            HKURLManager.Environments.values().forEach { environment: HKURLManager.Environments ->
                HKTestingFragment.add(environment.name, environment.map[HKURLManager.KEY_HOST] ?: "", HKURLManager.curEnvironment == environment)
            }
            RxBus.toObservable(HKTestingFragment.ChangeEvent::class.java).subscribe { changeEvent ->
                try {
                    HKURLManager.curEnvironment = HKURLManager.Environments.valueOf(changeEvent.model.name)
                } catch (_: Exception) {
                }
                HKToastUtil.show("检测到环境切换(${changeEvent.model.name})\n已切换到:${HKURLManager.curEnvironment.name}")
            }
            val notificationId = 999999
            HKTestingFragment.showDebugNotification(notificationId)
            RxBus.toObservable(HKActivityLifecycleCallbacks.ForegroundEvent::class.java).subscribe { foregroundEvent ->
                if (foregroundEvent.isApplicationInForeground)
                    HKTestingFragment.showDebugNotification(notificationId)
                else
                    HKTestingFragment.cancelDebugNotification(notificationId)
            }
        }
    }

    fun <T> compose(): ObservableTransformer<HKResponseModel<T>, T> {
        return ObservableTransformer { observable ->
            observable
                .map { responseModel: HKResponseModel<T>? ->
                    if (responseModel != null && responseModel.errorCode == 0 && responseModel.result != null) {
                        responseModel.result!!
                    } else {
                        throw HKRetrofitServerException(responseModel?.errorCode ?: -1, responseModel?.errorMessage ?: "网络不给力")
                    }
                }
                .onErrorResumeNext { throwable: Throwable ->
                    Observable.error(HKRetrofitException.handleException(throwable))
                }
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        }
    }

    /**
     * 普通的 ResponseBody 格式(非json), 处理 404 等 errorCode
     */
    fun <T> composeWithoutResponse(): ObservableTransformer<T, T> {
        return ObservableTransformer { observable ->
            observable.onErrorResumeNext { throwable: Throwable ->
                Observable.error(HKRetrofitException.handleException(throwable))
            }
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        }
    }

    /**
     * 接口数据缓存策略:
     *      1.检查缓存数据, 若有则先显示缓存
     *      2.回调网络数据, 若成功则覆盖先前的缓存数据
     *      3.拉取数据成功, 缓存数据到本体
     *
     * @param key 接口标识, 如: "接口名" + "请求参数(Json格式)"
     */
    fun <T> cache(key: String): ObservableTransformer<in T, out T>? {
        return ObservableTransformer { observable ->
            Observable.create<T> { emitter ->
                // 1.检查缓存数据, 若有则先显示缓存
                val localData = HKCacheFileManager.get(HKBaseApplication.INSTANCE).getAsObject(key)
                if (localData != null)
                    emitter.onNext(localData as T)

                // 2.回调网络数据, 若成功则覆盖先前的缓存数据
                observable.subscribe({ data ->
                    emitter.onNext(data)

                    // 3.拉取数据成功, 缓存数据到本体
                    HKCacheFileManager.get(HKBaseApplication.INSTANCE).put(key, data as Serializable)

                }, { error ->
                    emitter.onError(error)

                }, {
                    emitter.onComplete()
                })
            }
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        }
    }

    private val builder: Retrofit.Builder  by lazy {
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())

    }

    @Synchronized
    fun <T> getApi(clazz: Class<T>): T =
        builder.baseUrl(HKURLManager.curHost).client(HKOkHttpManager.client).build().create(clazz)
}
