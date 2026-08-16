import { Client, type IMessage } from "@stomp/stompjs";
import { WS_URL } from "../config/api";

export interface Notification {
  id: string;
  userId: string;
  message: string;
  type: string;
  read: boolean;
  createdAt: string;
}

export interface TaskSnapshot {
  taskId: string;
  title: string;
  description: string;
  deadline: string;
  status: string;
  assignedToId: string;
  assignedToName: string;
  createdById: string;
  createdByName: string;
  updatedAt: string;
}

export type TaskEvent =
  | { type: "upsert"; task: TaskSnapshot }
  | { type: "removed"; taskId: string };

let client: Client | null = null;
let hasConnectedBefore = false;

export function connectRealtime(
  onNotification: (notification: Notification) => void,
  onTaskEvent: (event: TaskEvent) => void,
  onReconnect?: () => void
) {
  const token = localStorage.getItem("token");
  if (!token) return;

  hasConnectedBefore = false;

  client = new Client({
    brokerURL: `${WS_URL.replace(/^http/, "ws")}/ws`,
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      client!.subscribe("/user/queue/notifications", (message: IMessage) => {
        onNotification(JSON.parse(message.body));
      });
      client!.subscribe("/user/queue/tasks", (message: IMessage) => {
        onTaskEvent(JSON.parse(message.body));
      });

      if (hasConnectedBefore) {
        onReconnect?.();
      }
      hasConnectedBefore = true;
    },
  });

  client.activate();
}

export function disconnectRealtime() {
  client?.deactivate();
  client = null;
  hasConnectedBefore = false;
}
