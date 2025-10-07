interface Task {
  id: string;
  title: string;
  description: string;
  status: string;
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
  if (loading) return <div className="text-center text-gray-500">Loading...</div>;
  if (tasks.length === 0) return <div className="text-center text-gray-400">No tasks yet.</div>;

  return (
    <ul className="space-y-3">
      {tasks.map((t) => (
        <li
          key={t.id}
          className="border rounded-lg p-4 flex justify-between items-center"
        >
          <div>
            <p className="font-semibold">{t.title}</p>
            <p className="text-sm text-gray-500">{t.description}</p>
            <p
              className={`text-xs font-bold ${
                t.status === "DONE" ? "text-green-600" : "text-yellow-600"
              }`}
            >
              {t.status}
            </p>
          </div>
          <div className="space-x-2">
            {t.status !== "DONE" && (
              <button
                onClick={() => onMarkDone(t.id)}
                className="text-sm text-white bg-green-500 px-3 py-1 rounded-lg"
              >
                Done
              </button>
            )}
            <button
              onClick={() => onDelete(t.id)}
              className="text-sm text-white bg-gray-400 px-3 py-1 rounded-lg"
            >
              Delete
            </button>
          </div>
        </li>
      ))}
    </ul>
  );
}
