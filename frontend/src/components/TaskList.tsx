import {CheckCircle, Trash2, CalendarDays, User, Clock} from "lucide-react"
interface Task {
  id: string;
  title: string;
  description: string;
  status: string;
  createdById: string;
  createdByName: string;
  assignedToId:string;
  assignedToName: string;
  deadline: string;
}

interface TaskListProps {
  tasks: Task[];
  loading: boolean;
  onMarkDone: (id: string) => void;
  onDelete: (id: string) => void;
}

export default function TaskList({
  tasks,
  loading,
  onMarkDone,
  onDelete,
}: TaskListProps) {
  if (loading) return (<div className="text-center text-gray-400 animate-pulse py-6">Loading tasks...</div>);
  if (tasks.length === 0) return (<div className="text-center text-gray-500 py-6 italic">No tasks, take a break ☕.</div>);
  const getAccentColor = (status: string) => {
    switch (status) {
      case "DONE":
        return "bg-green-500";
      case "IN_PROGRESS":
        return "bg-yellow-500";
      default:
        return "bg-gray-600";
    }
  };

  const isOverdue = (deadline?: string, status?: string) => {
    if (!deadline || status === "DONE") return false;
    return new Date(deadline).getTime() < Date.now();
  };
  const formatDate = (dateString?: string) => {
    if (!dateString) return "—";
    const date = new Date(dateString);
    return date.toLocaleDateString(undefined, {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

return (
    <ul className="space-y-4">
      {tasks.map((t) => {
        const overdue = isOverdue(t.deadline, t.status);

        return (
          <li
            key={t.id}
            className={`group relative border border-gray-700 rounded-xl flex justify-between items-start transition-all duration-200 overflow-hidden ${
              t.status === "DONE"
                ? "bg-gray-700/60 opacity-80"
                : "bg-gray-800/70 hover:bg-gray-700/70"
            }`}
          >
            {/* Accent color bar */}
            <div
              className={`absolute left-0 top-0 h-full w-1.5 ${getAccentColor(
                t.status
              )}`}
            />

            {/* Content */}
            <div className="flex-1 p-5 pl-6">
              <div className="flex items-center gap-2">
                <p className="text-lg font-semibold text-white">{t.title}</p>
                <span
                  className={`px-2 py-0.5 rounded-full text-xs font-semibold ${
                    t.status === "DONE"
                      ? "bg-green-600/30 text-green-300 border border-green-600/40"
                      : "bg-yellow-600/20 text-yellow-400 border border-yellow-600/40"
                  }`}
                >
                  {t.status}
                </span>
                {overdue && (
                  <span className="flex items-center gap-1 text-xs text-red-400 font-semibold">
                    <Clock className="h-3.5 w-3.5" />
                    Overdue
                  </span>
                )}
              </div>

              {t.description && (
                <p className="text-sm text-gray-400 mt-1">{t.description}</p>
              )}

              <div className="flex items-center gap-4 mt-3 text-sm text-gray-400">
                {t.deadline && (
                  <span
                    className={`flex items-center gap-1 ${
                      overdue ? "text-red-400 font-medium" : "text-gray-400"
                    }`}
                  >
                    <CalendarDays className="h-4 w-4" />
                    {formatDate(t.deadline)}
                  </span>
                )}
                {t.assignedToName && (
                  <span className="flex items-center gap-1">
                    <User className="h-4 w-4 text-gray-400" />
                    Assigned to{" "}
                    <span className="font-semibold text-gray-300"> {t.assignedToName}</span>
                  </span>
                )}

                {t.createdByName && (
                  <span className="flex items-center gap-1">
                    <User className="h-4 w-4 text-gray-400" />
                    Created by{" "}
                    <span className="font-semibold text-gray-300">
                      {t.createdByName}
                    </span>
                  </span>
                )}
              </div>
            </div>

            {/* Buttons */}
            <div className="flex items-center gap-2 pr-5 pt-5">
              {t.status !== "DONE" && (
                <button
                  onClick={() => onMarkDone(t.id)}
                  className="flex items-center gap-1 text-sm text-white bg-green-600/90 hover:bg-green-500 transition-colors px-3 py-1.5 rounded-lg shadow-sm"
                >
                  <CheckCircle className="h-4 w-4" /> Done
                </button>
              )}
              <button
                onClick={() => onDelete(t.id)}
                className="flex items-center gap-1 text-sm text-white bg-red-700/90 hover:bg-red-600 transition-colors px-3 py-1.5 rounded-lg shadow-sm"
              >
                <Trash2 className="h-4 w-4" /> Delete
              </button>
            </div>
          </li>
        );
      })}
    </ul>
  );
}