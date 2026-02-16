import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, BehaviorSubject } from 'rxjs';
import { Client, Message } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from 'src/environments/environment';
import { map } from 'rxjs/operators'
import {
  PremiereSession,
  PlaybackState,
  PremiereEvent,
  CreatePremiereRequest,
  ChatMessage,
  ChatMessageType
} from '../models/premiere';

@Injectable({
  providedIn: 'root'
})
export class PremiereService {
  private apiUrl = environment.apiUrl + '/premieres';
  // ✅ CORRECT - SockJS needs http:// URLs
  private wsUrl = environment.wsUrl + '/premiere';

  private stompClient?: Client;
  private connected$ = new BehaviorSubject<boolean>(false);
  private playbackState$ = new Subject<PlaybackState>();
  private premiereEvents$ = new Subject<PremiereEvent>();

  constructor(private http: HttpClient) {}

  // ============ API Methods ============

  createPremiere(request: CreatePremiereRequest): Observable<PremiereSession> {
    return this.http.post<PremiereSession>(this.apiUrl, request);
  }

  getPremiere(id: number): Observable<PremiereSession> {
    return this.http.get<PremiereSession>(`${this.apiUrl}/${id}`);
  }

  getPlaybackState(id: number): Observable<PlaybackState> {
    return this.http.get<PlaybackState>(`${this.apiUrl}/${id}/state`);
  }

  getLivePremieres(): Observable<PremiereSession[]> {
    return this.http.get<PremiereSession[]>(`${this.apiUrl}/live`);
  }

  getUpcomingPremieres(): Observable<PremiereSession[]> {
    return this.http.get<any>(`${this.apiUrl}/upcoming?page=0&size=50`).pipe(
      map(response => response.content || response.data || [])
    );
  }

  // ============ WebSocket Methods ============

  connect(): void {
    if (this.stompClient?.connected) {
      console.log('Already connected to WebSocket');
      return;
    }

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(this.wsUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => console.log('STOMP:', str),
      onConnect: () => {
        console.log('Connected to premiere WebSocket');
        this.connected$.next(true);
      },
      onDisconnect: () => {
        console.log('Disconnected from premiere WebSocket');
        this.connected$.next(false);
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
      }
    });

    this.stompClient.activate();
  }

  disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.connected$.next(false);
    }
  }

  isConnected(): Observable<boolean> {
    return this.connected$.asObservable();
  }

  // Subscribe to playback state updates for a premiere
  subscribeToPlayback(premiereId: number): void {
    if (!this.stompClient?.connected) {
      console.error('Cannot subscribe: WebSocket not connected');
      return;
    }

    this.stompClient.subscribe(
      `/topic/premiere/${premiereId}/playback`,
      (message: Message) => {
        const state: PlaybackState = JSON.parse(message.body);
        this.playbackState$.next(state);
      }
    );
  }

  // Subscribe to premiere events
  subscribeToEvents(premiereId: number): void {
    if (!this.stompClient?.connected) {
      console.error('Cannot subscribe: WebSocket not connected');
      return;
    }

    this.stompClient.subscribe(
      `/topic/premiere/${premiereId}/events`,
      (message: Message) => {
        const event: PremiereEvent = JSON.parse(message.body);
        this.premiereEvents$.next(event);
      }
    );
  }

  // Observable for playback state updates
  getPlaybackStateUpdates(): Observable<PlaybackState> {
    return this.playbackState$.asObservable();
  }

  // Observable for premiere events
  getPremiereEventUpdates(): Observable<PremiereEvent> {
    return this.premiereEvents$.asObservable();
  }

  // Join a premiere
  joinPremiere(premiereId: number, userId: string): void {
    if (!this.stompClient?.connected) {
      console.error('Cannot join: WebSocket not connected');
      return;
    }

    this.stompClient.publish({
      destination: `/app/premiere/${premiereId}/join`,
      body: JSON.stringify({ userId })
    });
  }

  // Leave a premiere
  leavePremiere(premiereId: number, userId: string): void {
    if (!this.stompClient?.connected) {
      console.error('Cannot leave: WebSocket not connected');
      return;
    }

    this.stompClient.publish({
      destination: `/app/premiere/${premiereId}/leave`,
      body: JSON.stringify({ userId })
    });
  }

  // Send heartbeat
  sendHeartbeat(premiereId: number): void {
    if (!this.stompClient?.connected) {
      return;
    }

    this.stompClient.publish({
      destination: `/app/premiere/${premiereId}/heartbeat`,
      body: '{}'
    });
  }

  // Helper to compute latency-adjusted position
  computeAdjustedPosition(state: PlaybackState): number {
    if (!state.playing) {
      return state.currentPosition;
    }

    const clientReceiveTime = Date.now();
    const latency = (clientReceiveTime - state.serverTimestamp) / 1000;
    return state.currentPosition + latency;
  }

  private chatMessages$ = new Subject<ChatMessage>();

// Subscribe to chat messages
  subscribeToChat(premiereId: number): void {
    if (!this.stompClient?.connected) {
      console.error('Cannot subscribe: WebSocket not connected');
      return;
    }

    this.stompClient.subscribe(
      `/topic/premiere/${premiereId}/chat`,
      (message: Message) => {
        const chatMessage: ChatMessage = JSON.parse(message.body);
        this.chatMessages$.next(chatMessage);
      }
    );

    console.log(`📬 Subscribed to chat for premiere ${premiereId}`);
  }

// Observable for chat messages
  getChatMessages(): Observable<ChatMessage> {
    return this.chatMessages$.asObservable();
  }

// Send chat message
  sendChatMessage(premiereId: number, userId: string, username: string, message: string): void {
    if (!this.stompClient?.connected) {
      console.error('Cannot send chat: WebSocket not connected');
      return;
    }

    this.stompClient.publish({
      destination: `/app/premiere/${premiereId}/chat`,
      body: JSON.stringify({
        userId,
        username,
        message
      })
    });
  }
}
