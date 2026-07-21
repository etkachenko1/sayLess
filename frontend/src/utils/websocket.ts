import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";

export interface Notification {
  id: string;
  userId: string;
  message: string;
  type: string;
  read: boolean;
  createdAt: string;
}

const NOTIFICATION_SERVICE_URL = "http://localhost:8085";

let client: Client | null = null;

export function connectNotifications(onMessage: (notification: Notification) => void) {
  const token = localStorage.getItem("token");
  if (!token) return;

  client = new Client({
    webSocketFactory: () => new SockJS(`${NOTIFICATION_SERVICE_URL}/ws`),
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