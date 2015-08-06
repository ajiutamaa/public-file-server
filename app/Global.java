import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.lang.reflect.Method;
import java.net.URL;

/**
 * Created by rezkaaufar on 16/06/2015.
 */
public class Global extends GlobalSettings {
    @Override
    public void onStart(Application application) {
        super.beforeStart(application);
        Logger.debug("** start app **");
    }

    @Override
    public Action onRequest(Http.Request request, Method method) {
        return new ActionWrapper(super.onRequest(request, method));
    }

    /** for CORS */
    private class ActionWrapper extends Action.Simple {
        public ActionWrapper(Action<?> action) {
            this.delegate = action;
        }

        @Override
        public F.Promise<Result> call(Http.Context context) throws Throwable {
            F.Promise<Result> result = this.delegate.call(context);
            Http.Response response = context.response();
            response.setHeader("Access-Control-Allow-Origin", "*");
            return result;
        }
    }
}
