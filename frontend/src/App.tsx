import React from 'react';
import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import './App.css';
import { Navbar } from './components/Navbar';
import { Sidebar } from './components/Sidebar';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { ScheduleManagementPage } from './pages/ScheduleManagementPage';
import { MySchedulePage } from './pages/MySchedulePage';
import { CompliancePage } from './pages/CompliancePage';
import { AttendancePage } from './pages/AttendancePage';
import { ReportsPage } from './pages/ReportsPage';
import { AuditLogsPage } from './pages/AuditLogsPage';
import { EmployeesPage } from './pages/EmployeesPage';
import { DepartmentsPage } from './pages/DepartmentsPage';
import { SkillsPage } from './pages/SkillsPage';
import { ShiftTemplatesPage } from './pages/ShiftTemplatesPage';
import { StaffingRequirementsPage } from './pages/StaffingRequirementsPage';
import { AvailabilityPage } from './pages/AvailabilityPage';
import { LeaveManagementPage } from './pages/LeaveManagementPage';
import { RefreshCw } from 'lucide-react';

const ProtectedLayout: React.FC = () => {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center text-white gap-3">
        <RefreshCw className="w-6 h-6 animate-spin text-sky-500" />
        <span className="font-semibold text-sm">Loading MetroCare Scheduling System...</span>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />
      <div className="flex flex-1">
        <Sidebar />
        <main className="flex-1 p-8 max-w-7xl mx-auto w-full">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          <Route element={<ProtectedLayout />}>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/schedules" element={<ScheduleManagementPage />} />
            <Route path="/my-schedule" element={<MySchedulePage />} />
            <Route path="/compliance" element={<CompliancePage />} />
            <Route path="/attendance" element={<AttendancePage />} />
            <Route path="/reports" element={<ReportsPage />} />
            <Route path="/audit-logs" element={<AuditLogsPage />} />

            <Route path="/employees" element={<EmployeesPage />} />
            <Route path="/departments" element={<DepartmentsPage />} />
            <Route path="/skills" element={<SkillsPage />} />
            <Route path="/shift-templates" element={<ShiftTemplatesPage />} />
            <Route path="/staffing-requirements" element={<StaffingRequirementsPage />} />
            <Route path="/availability" element={<AvailabilityPage />} />
            <Route path="/leave" element={<LeaveManagementPage />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
