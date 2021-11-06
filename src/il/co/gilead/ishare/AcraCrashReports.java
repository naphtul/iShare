package il.co.gilead.ishare;

import android.app.Application;
import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(
        formKey = "",
        formUri = "http://naphtul.iriscouch.com/acra-ishare/_design/acra-storage/_update/report",
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin="reporteri",
        formUriBasicAuthPassword="sdlkfFXTDfg336b3D")
public class AcraCrashReports extends Application {
	@Override
	public void onCreate() {
	//  public final void onCreate() {
		super.onCreate();
		
		// The following line triggers the initialization of ACRA
		ACRA.init(this);
	}
}

