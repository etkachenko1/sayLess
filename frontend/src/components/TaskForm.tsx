import { Datepicker } from "flowbite-react";

interface TaskFormProps {
  title: string
  description: string
  assignedTo: string
  deadline: string
  onTitleChange: (v: string) => void
  onDescriptionChange: (v: string) => void
  onAssignedToChange: (v: string) => void
  onDeadlineChange: (v: string) => void
  onSubmit: () => void
}

export default function TaskForm({
  title,
  description,
  assignedTo,
  deadline,
  onTitleChange,
  onDescriptionChange,
  onAssignedToChange,
  onDeadlineChange,
  onSubmit,
}: TaskFormProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mb-6">
      <div className="flex flex-col">
        <label className="text-sm font-medium mb-1">Title</label>
      <input
        value={title}
        onChange={(e) => onTitleChange(e.target.value)}
        placeholder="Task title"
        className="border rounded-lg p-3"
      />
      </div>
      <div className="flex flex-col">
      <label className="text-sm font-medium mb-1">Assign to</label>
      <input
        value={assignedTo}
        onChange={(e) => onAssignedToChange(e.target.value)}
        placeholder="Assign to (friends dropdown)"
        className="border rounded-lg p-3"
        disabled
      />
      </div>
      <div className="flex flex-col col-span-2">
        <label className="text-sm font-medium mb-1">Description</label>
        <input
        value={description}
        onChange={(e) => onDescriptionChange(e.target.value)}
        placeholder="Description"
        className="border rounded-lg p-3 col-span-2"
      />
      </div>
       {/* Deadline (Date) */}
      <div className="flex flex-col col-span-2">
        <label className="text-sm font-medium mb-1">Deadline</label>
        <div className="flex flex-col sm:flex-row items-center gap-3">
          <div className="border rounded-lg p-2 bg-gray-50 flex-1">
            <Datepicker
              onChange={(date: Date | null) => {
                if (date) {
                  const current = deadline ? new Date(deadline) : new Date();
                  date.setHours(current.getHours(), current.getMinutes());
                  onDeadlineChange(date.toISOString());
                }
              }}
              language="en"
              labelTodayButton="Today"
              labelClearButton="Clear"
            />
          </div>

          {/* Time input */}
          <input
            type="time"
            value={deadline ? new Date(deadline).toTimeString().slice(0, 5) : ""}
            onChange={(e) => {
              const [hours, minutes] = e.target.value.split(":");
              const newDate = deadline ? new Date(deadline) : new Date();
              newDate.setHours(parseInt(hours), parseInt(minutes));
              onDeadlineChange(newDate.toISOString());
            }}
            className="border rounded-lg p-3 bg-gray-50 focus:ring-2 focus:ring-red-400 w-40"
          />
        </div>

        {deadline && (
          <p className="text-xs text-gray-500 mt-2">
            Deadline:{" "}
            {new Date(deadline).toLocaleString(undefined, {
              dateStyle: "medium",
              timeStyle: "short",
            })}
          </p>
        )}
      </div>

      <button
        onClick={onSubmit}
        className="bg-red-500 text-white px-4 py-2 rounded-lg hover:bg-red-600 col-span-2"
      >
        Create Task
      </button>
    </div>
  )
}
