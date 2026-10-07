import React, { useState, useEffect } from 'react';
import { FileText, Building, RefreshCw, AlertCircle } from 'lucide-react';

interface Department {
  id: string;
  name: string;
}

interface CoverageReport {
  departmentId: string;
  departmentName: string;
  startDate: string;
  endDate: string;
  totalRequiredSlots: number;
  totalAssignedSlots: number;
  totalShortageSlots: number;
  coveragePercentage: number;
  shortagesByReason: Record<string, number>;
  dailyBreakdown: {
    date: string;
    required: number;
    assigned: number;
    shortage: number;
    coveragePercent: number;
  }[];
}

interface OvertimeReport {
  departmentId: string;
  departmentName: string;
  startDate: string;
  endDate: string;
  totalOvertimeHours: number;
  employeeSummaries: {
    employeeId: string;
    employeeName: string;
    employeeType: string;
    plannedHours: number;
    actualHours: number;
    overtimeHours: number;
    overtimeShiftCount: number;
  }[];
}

interface FairnessReport {
  departmentId: string;
  departmentName: string;
  totalShiftsVariance: number;
  nightShiftsVariance: number;
  weekendShiftsVariance: number;
  employeeMetrics: {
    employeeId: string;
    employeeName: string;
    employeeType: string;
    totalShiftCount: number;
    nightShiftCount: number;
    weekendShiftCount: number;
  }[];
}

export const ReportsPage: React.FC = () => {
  const [departments, setDepartments] = useState<Department[]>([]);
  const [selectedDeptId, setSelectedDeptId] = useState<string>('');
  const [activeTab, setActiveTab] = useState<'coverage' | 'overtime' | 'fairness'>('coverage');

  const [coverageData, setCoverageData] = useState<CoverageReport | null>(null);
  const [overtimeData, setOvertimeData] = useState<OvertimeReport | null>(null);
  const [fairnessData, setFairnessData] = useState<FairnessReport | null>(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDepartments();
  }, []);

  useEffect(() => {
    fetchReportData();
  }, [selectedDeptId, activeTab]);

  const fetchDepartments = async () => {
    try {
      const token = localStorage.getItem('access_token');
      const res = await fetch('/api/departments', {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.ok) {
        setDepartments(await res.json());
      }
    } catch (e) {
      console.error(e);
    }
  };

  const fetchReportData = async () => {
    setLoading(true);
    setError(null);
    try {
      const token = localStorage.getItem('access_token');
      const headers = { Authorization: `Bearer ${token}` };
      const deptQuery = selectedDeptId ? `?departmentId=${selectedDeptId}` : '';

      if (activeTab === 'coverage') {
        const res = await fetch(`/api/reports/coverage${deptQuery}`, { headers });
        if (res.ok) setCoverageData(await res.json());
        else setError('Failed to load coverage report');
      } else if (activeTab === 'overtime') {
        const res = await fetch(`/api/reports/overtime${deptQuery}`, { headers });
        if (res.ok) setOvertimeData(await res.json());
        else setError('Failed to load overtime summary report');
      } else if (activeTab === 'fairness') {
        const res = await fetch(`/api/reports/fairness${deptQuery}`, { headers });
        if (res.ok) setFairnessData(await res.json());
        else setError('Failed to load fairness metrics report');
      }
    } catch (e) {
      setError('Network error fetching report data');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <FileText className="w-6 h-6 text-sky-600" />
            <span>Workforce & Analytics Reports</span>
          </h2>
          <p className="text-xs text-slate-500 font-medium">
            Staffing coverage percentages, overtime summary, and CP-SAT fairness metrics
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Building className="w-4 h-4 text-slate-400" />
          <select
            value={selectedDeptId}
            onChange={(e) => setSelectedDeptId(e.target.value)}
            className="px-3.5 py-2 bg-white border border-slate-300 rounded-xl text-xs font-semibold focus:ring-2 focus:ring-sky-500"
          >
            <option value="">All Departments</option>
            {departments.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Report Tabs */}
      <div className="flex gap-2 border-b border-slate-200 pb-2">
        <button
          onClick={() => setActiveTab('coverage')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
            activeTab === 'coverage' ? 'bg-sky-600 text-white shadow-md' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
          }`}
        >
          Staffing Coverage Report
        </button>
        <button
          onClick={() => setActiveTab('overtime')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
            activeTab === 'overtime' ? 'bg-sky-600 text-white shadow-md' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
          }`}
        >
          Overtime Summary
        </button>
        <button
          onClick={() => setActiveTab('fairness')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
            activeTab === 'fairness' ? 'bg-sky-600 text-white shadow-md' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
          }`}
        >
          Fairness Variance Metrics
        </button>
      </div>

      {error && (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl text-sm flex items-center gap-2">
          <AlertCircle className="w-5 h-5 text-rose-600" />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="glass-card p-12 text-center text-slate-500 rounded-2xl flex items-center justify-center gap-2">
          <RefreshCw className="w-5 h-5 animate-spin text-sky-600" />
          <span className="text-sm font-semibold">Generating report metrics...</span>
        </div>
      ) : (
        <>
          {/* TAB 1: Coverage Report */}
          {activeTab === 'coverage' && coverageData && (
            <div className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div className="glass-card p-5 rounded-2xl">
                  <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Coverage Rate</p>
                  <h3 className="text-2xl font-bold text-slate-900 mt-1">{coverageData.coveragePercentage}%</h3>
                  <div className="w-full bg-slate-200 rounded-full h-2 mt-2 overflow-hidden">
                    <div
                      className="bg-sky-600 h-2 rounded-full transition-all"
                      style={{ width: `${Math.min(100, coverageData.coveragePercentage)}%` }}
                    ></div>
                  </div>
                </div>

                <div className="glass-card p-5 rounded-2xl">
                  <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Required Slots</p>
                  <h3 className="text-2xl font-bold text-slate-900 mt-1">{coverageData.totalRequiredSlots}</h3>
                </div>

                <div className="glass-card p-5 rounded-2xl">
                  <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Filled Slots</p>
                  <h3 className="text-2xl font-bold text-emerald-600 mt-1">{coverageData.totalAssignedSlots}</h3>
                </div>

                <div className="glass-card p-5 rounded-2xl">
                  <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Unfilled Shortages</p>
                  <h3 className="text-2xl font-bold text-rose-600 mt-1">{coverageData.totalShortageSlots}</h3>
                </div>
              </div>

              <div className="glass-card p-6 rounded-2xl space-y-4">
                <h3 className="text-base font-bold text-slate-900">Daily Coverage Breakdown</h3>
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs border-collapse">
                    <thead>
                      <tr className="bg-slate-100 text-slate-700 font-bold uppercase tracking-wider border-b border-slate-200">
                        <th className="p-3">Date</th>
                        <th className="p-3">Required</th>
                        <th className="p-3">Assigned</th>
                        <th className="p-3">Shortage</th>
                        <th className="p-3">Percentage</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200">
                      {coverageData.dailyBreakdown.map((row) => (
                        <tr key={row.date} className="hover:bg-slate-50">
                          <td className="p-3 font-bold text-slate-800">{row.date}</td>
                          <td className="p-3 font-semibold">{row.required}</td>
                          <td className="p-3 text-emerald-700 font-semibold">{row.assigned}</td>
                          <td className="p-3 text-rose-700 font-semibold">{row.shortage}</td>
                          <td className="p-3 font-bold">{row.coveragePercent}%</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: Overtime Report */}
          {activeTab === 'overtime' && overtimeData && (
            <div className="space-y-6">
              <div className="glass-card p-5 rounded-2xl max-w-sm">
                <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Total Overtime Hours</p>
                <h3 className="text-2xl font-bold text-amber-600 mt-1">{overtimeData.totalOvertimeHours} Hours</h3>
              </div>

              <div className="glass-card p-6 rounded-2xl space-y-4">
                <h3 className="text-base font-bold text-slate-900">Employee Overtime Breakdown</h3>
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs border-collapse">
                    <thead>
                      <tr className="bg-slate-100 text-slate-700 font-bold uppercase tracking-wider border-b border-slate-200">
                        <th className="p-3">Employee</th>
                        <th className="p-3">Type</th>
                        <th className="p-3">Planned Hours</th>
                        <th className="p-3">Actual Hours</th>
                        <th className="p-3">Overtime Hours</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200">
                      {overtimeData.employeeSummaries.map((emp) => (
                        <tr key={emp.employeeId} className="hover:bg-slate-50">
                          <td className="p-3 font-bold text-slate-900">{emp.employeeName}</td>
                          <td className="p-3"><span className="badge badge-info">{emp.employeeType}</span></td>
                          <td className="p-3 font-semibold">{emp.plannedHours}h</td>
                          <td className="p-3 font-semibold">{emp.actualHours}h</td>
                          <td className="p-3 font-bold text-amber-700">{emp.overtimeHours}h ({emp.overtimeShiftCount} shifts)</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: Fairness Metrics Report */}
          {activeTab === 'fairness' && fairnessData && (
            <div className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="glass-card p-5 rounded-2xl">
                  <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Total Shifts Variance</p>
                  <h3 className="text-2xl font-bold text-slate-900 mt-1">{fairnessData.totalShiftsVariance}</h3>
                  <p className="text-[11px] text-slate-500 mt-1">Lower is fairer across staff</p>
                </div>

                <div className="glass-card p-5 rounded-2xl">
                  <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Night Shift Variance</p>
                  <h3 className="text-2xl font-bold text-purple-600 mt-1">{fairnessData.nightShiftsVariance}</h3>
                </div>

                <div className="glass-card p-5 rounded-2xl">
                  <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Weekend Shift Variance</p>
                  <h3 className="text-2xl font-bold text-sky-600 mt-1">{fairnessData.weekendShiftsVariance}</h3>
                </div>
              </div>

              <div className="glass-card p-6 rounded-2xl space-y-4">
                <h3 className="text-base font-bold text-slate-900">Per-Staff Shift Distribution</h3>
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs border-collapse">
                    <thead>
                      <tr className="bg-slate-100 text-slate-700 font-bold uppercase tracking-wider border-b border-slate-200">
                        <th className="p-3">Employee</th>
                        <th className="p-3">Role</th>
                        <th className="p-3">Total Shifts</th>
                        <th className="p-3">Night Shifts</th>
                        <th className="p-3">Weekend Shifts</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200">
                      {fairnessData.employeeMetrics.map((m) => (
                        <tr key={m.employeeId} className="hover:bg-slate-50">
                          <td className="p-3 font-bold text-slate-900">{m.employeeName}</td>
                          <td className="p-3"><span className="badge badge-info">{m.employeeType}</span></td>
                          <td className="p-3 font-semibold">{m.totalShiftCount}</td>
                          <td className="p-3 font-semibold text-purple-700">{m.nightShiftCount}</td>
                          <td className="p-3 font-semibold text-sky-700">{m.weekendShiftCount}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};
