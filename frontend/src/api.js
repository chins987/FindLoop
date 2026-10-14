import { supabase } from "./lib/supabase";

// Matches your Spring Boot ItemReportController.
const BASE = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

async function call(path, options = {}) {
  const {
    data: { session },
    error,
  } = await supabase.auth.getSession();

  if (error) {
    throw new Error("Could not check login session. Please sign in again.");
  }

  const headers = {
    "Content-Type": "application/json",
    ...options.headers,
  };

  // Attach the Supabase access token when the user is signed in.
  if (session?.access_token) {
    headers.Authorization = `Bearer ${session.access_token}`;
  }

  const res = await fetch(BASE + path, {
    ...options,
    headers,
  });

  if (!res.ok) {
    let msg = `Request failed (${res.status})`;
    try {
      msg = (await res.json()).error || msg;
    } catch {
      // Keep the default error message.
    }
    throw new Error(msg);
  }

  if (res.status === 204) return null;

  const text = await res.text();
  if (!text.trim()) return null;

  try {
    return JSON.parse(text);
  } catch {
    throw new Error("The server returned an invalid response.");
  }
}

export const api = {
  // { type: "LOST"|"FOUND", title, description, category, location, locationDetails?, eventDateTime? }
  createReport: (report) =>
    call("/reports", {
      method: "POST",
      body: JSON.stringify(report),
    }),

  listReports: (type, filters = {}) => {
    const params = new URLSearchParams();
    if (type) params.set("type", type);
    if (filters.q?.trim()) params.set("q", filters.q.trim());
    if (filters.category) params.set("category", filters.category);
    if (filters.latitude != null && filters.longitude != null) {
      params.set("latitude", filters.latitude);
      params.set("longitude", filters.longitude);
      params.set("radiusKm", filters.radiusKm || "10");
    }
    return call(`/reports${params.size ? `?${params}` : ""}`);
  },

  getReport: (id) => call(`/reports/${id}`),

  updateReportStatus: (id, status) => call(`/reports/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  }),

  getNotificationPreferences: () => call("/notifications/preferences"),
  updateNotificationPreferences: (preferences) => call("/notifications/preferences", {
    method: "PUT",
    body: JSON.stringify(preferences),
  }),
  getNotifications: () => call("/notifications"),
  markNotificationRead: (id) => call(`/notifications/${id}/read`, { method: "PATCH" }),

  // Matching results for a report.
  getMatches: (id) => call(`/reports/${id}/matches`),
  getOwnershipQuestion: (id) => call(`/reports/${id}/ownership-check`),
  verifyOwnershipAnswer: (id, answer) => call(`/reports/${id}/ownership-check`, {
    method: "POST", body: JSON.stringify({ answer }),
  }),
  notAMatch: (sourceId, candidateId) => call(`/reports/${sourceId}/matches/${candidateId}/not-a-match`, { method: "POST" }),


  // Contact requests
  sendContactRequest: (reportId, ownershipReportId) =>
    call(`/contact-requests/${reportId}${ownershipReportId ? `?ownershipReportId=${ownershipReportId}` : ""}`, {
      method: "POST",
    }),

  getSentRequests: () => call("/contact-requests/sent"),

  getReceivedRequests: () => call("/contact-requests/received"),

  respondToRequest: (requestId, decision) =>
    call(`/contact-requests/${requestId}`, {
      method: "PATCH",
      body: JSON.stringify({ decision }),
    }),

  // Conversations and messages
  getConversations: () => call("/conversations"),

  openConversation: (requestId) =>
    call(`/conversations/from-request/${requestId}`, {
      method: "POST",
    }),

  getMessages: (conversationId) =>
    call(`/conversations/${conversationId}/messages`),

  sendMessage: (conversationId, content) =>
    call(`/conversations/${conversationId}/messages`, {
      method: "POST",
      body: JSON.stringify({ content }),
    }),

};
