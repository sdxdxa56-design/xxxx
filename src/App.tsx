import React, { useState } from 'react';
import { 
  ShieldAlert, 
  Terminal, 
  Smartphone, 
  Code2, 
  CheckCircle2, 
  AlertTriangle, 
  Cpu, 
  Layers, 
  Play, 
  Square, 
  Trash2, 
  Sparkles,
  Lock,
  Unlock
} from 'lucide-react';

export default function App() {
  const [isMonitoring, setIsMonitoring] = useState(true);
  const [hasRoot, setHasRoot] = useState(true);
  const [activeTab, setActiveTab] = useState<'monitor' | 'code' | 'root'>('monitor');
  const [errors, setErrors] = useState([
    {
      id: 1,
      tag: 'AndroidRuntime',
      type: 'FATAL EXCEPTION',
      app: 'com.example.bankingapp',
      message: 'java.lang.NullPointerException: Attempt to read from null array in SecurityValidator.kt:84',
      time: '14:32:10.421',
      severity: 'CRASH'
    },
    {
      id: 2,
      tag: 'ActivityManager',
      type: 'ANR (Application Not Responding)',
      app: 'com.messaging.chat',
      message: 'Input dispatching timed out (Waiting to send key event to com.messaging.chat/.ChatActivity)',
      time: '14:31:05.109',
      severity: 'ANR'
    },
    {
      id: 3,
      tag: 'SQLiteLog',
      type: 'DATABASE CORRUPTION',
      app: 'com.ecommerce.shop',
      message: 'android.database.sqlite.SQLiteDatabaseCorruptException: database disk image is malformed',
      time: '14:28:44.982',
      severity: 'ERROR'
    }
  ]);

  const clearErrors = () => {
    setErrors([]);
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      {/* Header */}
      <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur px-6 py-4 flex items-center justify-between sticky top-0 z-50">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-emerald-400">
            <ShieldAlert className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-xl font-bold bg-gradient-to-r from-emerald-400 to-teal-300 bg-clip-text text-transparent">
              App Inspector • مراقب التطبيقات
            </h1>
            <p className="text-xs text-slate-400">
              Kotlin Jetpack Compose • libsu Root Logcat Engine (Android 8 - 15)
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 bg-slate-800/80 border border-slate-700/60 rounded-lg px-3 py-1.5 text-xs">
            <span className={`w-2.5 h-2.5 rounded-full ${hasRoot ? 'bg-emerald-400 animate-pulse' : 'bg-amber-400'}`}></span>
            <span className="text-slate-300">
              {hasRoot ? 'Root Access: Active (libsu)' : 'Root Access: Standard (adb)'}
            </span>
          </div>

          <button
            onClick={() => setIsMonitoring(!isMonitoring)}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium text-sm transition ${
              isMonitoring 
                ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30 hover:bg-rose-500/30' 
                : 'bg-emerald-500 text-slate-950 font-semibold hover:bg-emerald-400'
            }`}
          >
            {isMonitoring ? (
              <>
                <Square className="w-4 h-4" /> إيقاف المراقبة
              </>
            ) : (
              <>
                <Play className="w-4 h-4" /> تشغيل المراقبة
              </>
            )}
          </button>
        </div>
      </header>

      {/* Navigation tabs */}
      <div className="border-b border-slate-800 bg-slate-900/40 px-6 flex gap-6">
        <button
          onClick={() => setActiveTab('monitor')}
          className={`py-3 text-sm font-medium border-b-2 flex items-center gap-2 transition ${
            activeTab === 'monitor'
              ? 'border-emerald-500 text-emerald-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Terminal className="w-4 h-4" /> مراقب السجلات المباشر (Live Logcat)
        </button>

        <button
          onClick={() => setActiveTab('root')}
          className={`py-3 text-sm font-medium border-b-2 flex items-center gap-2 transition ${
            activeTab === 'root'
              ? 'border-emerald-500 text-emerald-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Cpu className="w-4 h-4" /> حالة الروت والصلاحيات (Root Status)
        </button>

        <button
          onClick={() => setActiveTab('code')}
          className={`py-3 text-sm font-medium border-b-2 flex items-center gap-2 transition ${
            activeTab === 'code'
              ? 'border-emerald-500 text-emerald-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Code2 className="w-4 h-4" /> هيكل مشروع أندرويد (Kotlin Source)
        </button>
      </div>

      {/* Main Content Area */}
      <main className="flex-1 p-6 max-w-7xl mx-auto w-full">
        {activeTab === 'monitor' && (
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-lg font-bold text-slate-100 flex items-center gap-2">
                  <Terminal className="w-5 h-5 text-emerald-400" />
                  الأخطاء المسجلة حديثاً ({errors.length})
                </h2>
                <p className="text-xs text-slate-400">
                  قراءة فورية لجميع تطبيقات النظام وتطبيقات المستخدمين عبر `su -c logcat`
                </p>
              </div>

              {errors.length > 0 && (
                <button
                  onClick={clearErrors}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs transition border border-slate-700"
                >
                  <Trash2 className="w-3.5 h-3.5 text-rose-400" />
                  مسح كل السجلات
                </button>
              )}
            </div>

            {errors.length === 0 ? (
              <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-12 text-center">
                <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto mb-3 opacity-80" />
                <h3 className="text-base font-semibold text-slate-200">لا توجد أخطاء حالية</h3>
                <p className="text-xs text-slate-400 mt-1">الخدمة تعمل في الخلفية وتراقب النظام بالكامل</p>
              </div>
            ) : (
              <div className="grid gap-4">
                {errors.map((err) => (
                  <div 
                    key={err.id} 
                    className="bg-slate-900/80 border border-slate-800 hover:border-slate-700 transition rounded-xl p-5 shadow-lg relative overflow-hidden"
                  >
                    <div className="absolute top-0 right-0 left-0 h-1 bg-gradient-to-r from-rose-500 to-amber-500" />
                    
                    <div className="flex flex-wrap items-center justify-between gap-2 mb-3">
                      <div className="flex items-center gap-2">
                        <span className="px-2.5 py-1 rounded-md text-xs font-bold bg-rose-500/20 text-rose-400 border border-rose-500/30">
                          {err.severity}
                        </span>
                        <span className="text-xs font-mono bg-slate-800 px-2 py-1 rounded text-slate-300">
                          {err.app}
                        </span>
                        <span className="text-xs text-slate-400 font-mono">
                          [{err.tag}]
                        </span>
                      </div>
                      <span className="text-xs text-slate-500 font-mono">{err.time}</span>
                    </div>

                    <p className="text-sm font-mono text-rose-200/90 bg-slate-950/70 p-3 rounded-lg border border-slate-800/80">
                      {err.message}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'root' && (
          <div className="max-w-2xl mx-auto space-y-6">
            <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-6">
              <div className="flex items-center gap-4">
                <div className={`p-3 rounded-xl ${hasRoot ? 'bg-emerald-500/10 text-emerald-400' : 'bg-amber-500/10 text-amber-400'}`}>
                  {hasRoot ? <Unlock className="w-8 h-8" /> : <Lock className="w-8 h-8" />}
                </div>
                <div>
                  <h3 className="text-lg font-bold text-slate-100">حالة صلاحيات الروت (Superuser / Magisk / KernelSU)</h3>
                  <p className="text-xs text-slate-400">إدارة صلاحيات القراءة المباشرة لسجلات النظام بدون قيود</p>
                </div>
              </div>

              <div className="grid gap-3 border-t border-slate-800 pt-5">
                <div className="flex items-center justify-between p-3.5 bg-slate-950 rounded-xl border border-slate-800/80">
                  <span className="text-sm text-slate-300">Root Binary Available (`su`)</span>
                  <span className="text-xs font-bold px-2.5 py-1 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                    متوفر ✅
                  </span>
                </div>

                <div className="flex items-center justify-between p-3.5 bg-slate-950 rounded-xl border border-slate-800/80">
                  <span className="text-sm text-slate-300">libsu Root Permission</span>
                  <span className={`text-xs font-bold px-2.5 py-1 rounded ${hasRoot ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-amber-500/20 text-amber-400 border border-amber-500/30'}`}>
                    {hasRoot ? 'ممنوحة ✅' : 'غير ممنوحة ⚠️'}
                  </span>
                </div>
              </div>

              <button
                onClick={() => setHasRoot(!hasRoot)}
                className="w-full py-3 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold rounded-xl transition shadow-lg shadow-emerald-500/10"
              >
                {hasRoot ? 'إعادة فحص الصلاحية' : 'طلب صلاحية الروت (Grant Root Permission)'}
              </button>
            </div>
          </div>
        )}

        {activeTab === 'code' && (
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-4">
            <h3 className="text-base font-bold text-slate-200 flex items-center gap-2">
              <Layers className="w-5 h-5 text-emerald-400" />
              ملفات المشروع الأصلية (Android Native Files)
            </h3>
            <div className="font-mono text-xs text-slate-300 space-y-2 bg-slate-950 p-4 rounded-xl border border-slate-800">
              <p className="text-emerald-400">📁 app/src/main/java/com/appinspector/</p>
              <p className="pl-4">├── 📁 services/</p>
              <p className="pl-8">├── RootManager.kt (libsu bridge)</p>
              <p className="pl-8">├── RootLogcatReader.kt (su -c logcat stream)</p>
              <p className="pl-8">├── LogcatService.kt (Foreground Engine)</p>
              <p className="pl-8">└── ErrorAnalyzer.kt (Crash/ANR parser)</p>
              <p className="pl-4">├── 📁 presentation/</p>
              <p className="pl-8">├── 📁 home/ (HomeScreen.kt, HomeViewModel.kt)</p>
              <p className="pl-8">├── 📁 monitor/ (MonitorScreen.kt)</p>
              <p className="pl-8">└── 📁 permission/ (PermissionCheckScreen.kt)</p>
              <p className="pl-4">└── MainActivity.kt</p>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
