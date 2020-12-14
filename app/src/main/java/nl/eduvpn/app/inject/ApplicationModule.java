/*
 *  This file is part of eduVPN.
 *
 *     eduVPN is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     eduVPN is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with eduVPN.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.eduvpn.app.inject;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import javax.inject.Provider;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import nl.eduvpn.app.BuildConfig;
import nl.eduvpn.app.EduVPNApplication;
import nl.eduvpn.app.entity.CurrentVPN;
import nl.eduvpn.app.entity.OpenVPN;
import nl.eduvpn.app.entity.WireGuard;
import nl.eduvpn.app.service.APIService;
import nl.eduvpn.app.service.ConnectionService;
import nl.eduvpn.app.service.EduOpenVPNService;
import nl.eduvpn.app.service.HistoryService;
import nl.eduvpn.app.service.OrganizationService;
import nl.eduvpn.app.service.PreferencesService;
import nl.eduvpn.app.service.SecurityService;
import nl.eduvpn.app.service.SerializerService;
import nl.eduvpn.app.service.VPNService;
import nl.eduvpn.app.utils.Log;
import nl.eduvpn.app.wireguard.WireGuardService;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Application module providing the different dependencies
 * Created by Daniel Zolnai on 2016-10-07.
 */
@Module(includes = { ViewModelModule.class })
public class ApplicationModule {

    private final EduVPNApplication _application;

    public ApplicationModule(EduVPNApplication application) {
        _application = application;
    }

    @Provides
    @Singleton
    protected Context provideApplicationContext() {
        return _application.getApplicationContext();
    }

    @Provides
    @Singleton
    protected OrganizationService provideOrganizationService(SerializerService serializerService,
                                                             SecurityService securityService, OkHttpClient okHttpClient) {
        return new OrganizationService(serializerService, securityService, okHttpClient);
    }

    @Provides
    @Singleton
    protected SharedPreferences provideSecurePreferences(SecurityService securityService) {
        return securityService.getSecurePreferences();
    }

    @Provides
    @Singleton
    protected PreferencesService providePreferencesService(Context context, SerializerService serializerService, SharedPreferences sharedPreferences) {
        return new PreferencesService(context, serializerService, sharedPreferences);
    }

    @Provides
    @Singleton
    protected ConnectionService provideConnectionService(PreferencesService preferencesService, HistoryService historyService,
                                                         SecurityService securityService) {
        return new ConnectionService(preferencesService, historyService, securityService);
    }

    @Provides
    @Singleton
    protected APIService provideAPIService(ConnectionService connectionService, OkHttpClient okHttpClient) {
        return new APIService(connectionService, okHttpClient);
    }

    @Provides
    @Singleton
    protected SerializerService provideSerializerService() {
        return new SerializerService();
    }

    @Provides
    @Singleton
    protected EduOpenVPNService provideEduOpenVPNService(Context context, PreferencesService preferencesService) {
        return new EduOpenVPNService(context, preferencesService);
    }

    @Provides
    @Singleton
    protected WireGuardService provideWireGuardService(Context context) {
        return new WireGuardService(context);
    }

    @Provides
    protected VPNService provideVPNService(PreferencesService preferencesService,
                                           Provider<EduOpenVPNService> eduOpenVPNServiceProvider,
                                           Provider<WireGuardService> wireGuardServiceProvider) {
        CurrentVPN currentVPN = preferencesService.getCurrentVPN();
        if (currentVPN instanceof WireGuard) {
            return wireGuardServiceProvider.get();
        } else if (currentVPN instanceof OpenVPN) {
            return eduOpenVPNServiceProvider.get();
        } else {
            throw new RuntimeException("Could not determine what VPN service to use.");
        }
    }

    @Provides
    @Singleton
    protected HistoryService provideHistoryService(PreferencesService preferencesService) {
        return new HistoryService(preferencesService);
    }

    @Provides
    @Singleton
    protected SecurityService provideSecurityService(Context context) {
        return new SecurityService(context);
    }

    @Provides
    @Singleton
    protected OkHttpClient provideHttpClient(Context context) {
        File cacheDirectory = context.getCacheDir();
        long CACHE_SIZE = 16 * 1024 * 1024; // 16 Mb
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .cache(new Cache(cacheDirectory, CACHE_SIZE))
                .followSslRedirects(true)
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    try {
                        return chain.proceed(chain.request());
                    } catch (ConnectException | SocketTimeoutException | UnknownHostException ex) {
                        Log.d("OkHTTP", "Retrying request because previous one failed with connection exception...");
                        // Wait 3 seconds
                        try {
                            Thread.sleep(3_000);
                        } catch (InterruptedException e) {
                            // Do nothing
                        }
                        return chain.proceed(chain.request());
                    }
                });
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            clientBuilder.addInterceptor(logging);
        }
        return clientBuilder.build();
    }
}
