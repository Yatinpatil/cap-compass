# WorkManager instantiates this worker by class name.
-keep class com.paisatrack.app.work.BillReminderWorker { public <init>(...); }

# Manifest-instantiated SMS receiver in the smsReview flavor.
-keep class com.paisatrack.app.data.sms.IncomingSmsReceiver { public <init>(); }
