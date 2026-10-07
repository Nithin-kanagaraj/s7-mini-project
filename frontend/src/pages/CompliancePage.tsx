import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Award, Shield, Save, CheckCircle2, AlertCircle, RefreshCw, Info } from 'lucide-react';

interface ComplianceRule {
  id: string;
  ruleKey: string;
  ruleValue: number;
  description: string;
  updatedByUsername?: string;
  updatedAt?: string;
}

export const CompliancePage: React.FC = () => {
  const { isAdmin } = useAuth();
  const [rules, setRules] = useState<ComplianceRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [savingKey, setSavingKey] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Form edit states
  const [editValues, setEditValues] = useState<Record<string, number>>({});
  const [editDescriptions, setEditDescriptions] = useState<Record<string, string>>({});

  useEffect(() => {
    fetchRules();
  }, []);

  const fetchRules = async () => {
    setLoading(true);
    setError(null);
    try {
      const token = localStorage.getItem('access_token');
      const res = await fetch('/api/compliance/rules', {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (res.ok) {
        const data: ComplianceRule[] = await res.json();
        setRules(data);

        const initialVals: Record<string, number> = {};
        const initialDescs: Record<string, string> = {};
        data.forEach((r) => {
          initialVals[r.ruleKey] = r.ruleValue;
          initialDescs[r.ruleKey] = r.description || '';
        });
        setEditValues(initialVals);
        setEditDescriptions(initialDescs);
      } else {
        setError('Failed to fetch compliance rules from backend');
      }
    } catch (e) {
      setError('Network error loading compliance rules');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateRule = async (ruleKey: string) => {
    if (!isAdmin) {
      setError('Only System Administrators can modify compliance rules.');
      return;
    }

    setSavingKey(ruleKey);
    setError(null);
    setSuccessMsg(null);

    try {
      const token = localStorage.getItem('access_token');
      const res = await fetch(`/api/compliance/rules/${ruleKey}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          ruleValue: editValues[ruleKey],
          description: editDescriptions[ruleKey],
        }),
      });

      if (res.ok) {
        const updatedRule = await res.json();
        setRules((prev) => prev.map((r) => (r.ruleKey === ruleKey ? updatedRule : r)));
        setSuccessMsg(`Compliance rule '${ruleKey}' updated successfully!`);
      } else {
        const err = await res.json().catch(() => ({ message: 'Failed to update rule' }));
        setError(err.message || 'Failed to update rule');
      }
    } catch (e) {
      setError('Network error updating compliance rule');
    } finally {
      setSavingKey(null);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Award className="w-6 h-6 text-purple-600" />
            <span>Hospital Compliance & Labor Rules</span>
          </h2>
          <p className="text-xs text-slate-500 font-medium">
            System-wide constraints enforced during automated OR-Tools CP-SAT schedule generation
          </p>
        </div>

        <div className="flex items-center gap-2 px-3 py-1.5 bg-purple-50 border border-purple-200 rounded-xl text-purple-900 text-xs font-bold">
          <Shield className="w-4 h-4 text-purple-600" />
          <span>Admin Access Controlled</span>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl text-sm flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />
            <span>{error}</span>
          </div>
        </div>
      )}

      {successMsg && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-sm flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            <span>{successMsg}</span>
          </div>
        </div>
      )}

      {loading ? (
        <div className="glass-card p-12 text-center text-slate-500 rounded-2xl flex items-center justify-center gap-2">
          <RefreshCw className="w-5 h-5 animate-spin text-purple-600" />
          <span className="text-sm font-semibold">Loading compliance configuration...</span>
        </div>
      ) : (
        <div className="space-y-6">
          <div className="p-4 bg-slate-100 border border-slate-200 rounded-xl text-xs text-slate-700 flex items-start gap-3">
            <Info className="w-5 h-5 text-sky-600 shrink-0 mt-0.5" />
            <div>
              <span className="font-bold text-slate-900">How Compliance Rules work: </span>
              These rules directly govern the Google OR-Tools CP-SAT solver. Modifying values (e.g. changing Max Weekly Hours from 40 to 48) immediately alters the hard/soft constraints evaluated when generating new department schedules.
            </div>
          </div>

          <div className="grid grid-cols-1 gap-6">
            {rules.map((rule) => (
              <div key={rule.id} className="glass-card p-6 rounded-2xl space-y-4">
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 border-b border-slate-200 pb-3">
                  <div>
                    <h3 className="text-base font-bold text-slate-900 font-mono">{rule.ruleKey}</h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Last updated by <span className="font-semibold text-slate-700">{rule.updatedByUsername || 'System Admin'}</span>
                    </p>
                  </div>

                  <span className="badge badge-purple text-xs">
                    Current: {rule.ruleValue} {rule.ruleKey.includes('HOURS') ? 'Hours' : 'Shifts'}
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
                  <div>
                    <label className="block font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                      Numeric Limit Value
                    </label>
                    <input
                      type="number"
                      value={editValues[rule.ruleKey] ?? rule.ruleValue}
                      onChange={(e) =>
                        setEditValues((prev) => ({ ...prev, [rule.ruleKey]: Number(e.target.value) }))
                      }
                      disabled={!isAdmin}
                      className="w-full px-3.5 py-2 bg-slate-50 border border-slate-300 rounded-xl text-sm font-semibold focus:ring-2 focus:ring-purple-500 focus:outline-none disabled:opacity-60"
                    />
                  </div>

                  <div className="md:col-span-2">
                    <label className="block font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                      Rule Description & Impact
                    </label>
                    <input
                      type="text"
                      value={editDescriptions[rule.ruleKey] ?? rule.description}
                      onChange={(e) =>
                        setEditDescriptions((prev) => ({ ...prev, [rule.ruleKey]: e.target.value }))
                      }
                      disabled={!isAdmin}
                      className="w-full px-3.5 py-2 bg-slate-50 border border-slate-300 rounded-xl text-sm font-medium focus:ring-2 focus:ring-purple-500 focus:outline-none disabled:opacity-60"
                    />
                  </div>
                </div>

                {isAdmin && (
                  <div className="flex justify-end pt-2">
                    <button
                      onClick={() => handleUpdateRule(rule.ruleKey)}
                      disabled={savingKey === rule.ruleKey}
                      className="px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white font-semibold text-xs rounded-xl shadow-md transition-all flex items-center gap-2 cursor-pointer disabled:opacity-50"
                    >
                      {savingKey === rule.ruleKey ? (
                        <>
                          <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                          <span>Saving...</span>
                        </>
                      ) : (
                        <>
                          <Save className="w-3.5 h-3.5" />
                          <span>Save Rule Changes</span>
                        </>
                      )}
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
