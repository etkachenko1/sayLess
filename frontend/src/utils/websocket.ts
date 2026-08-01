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

let client: Client | null = null;

export function connectNotifications(onMessage: (notification: Notification) => void) {
  const token = localStorage.getItem("token");
  if (!token) return;

  client = new Client({
    brokerURL: `${WS_URL.replace(/^http/, "ws")}/ws`,
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5000,
    onConnect: () => {
      client!.subscribe("/user/queue/notifications", (message: IMessage) => {
        onMessage(JSON.parse(message.body));
      });
    },
  });

  client.activate();
}

export function disconnectNotifications() {
  client?.deactivate();
  client = null;
}