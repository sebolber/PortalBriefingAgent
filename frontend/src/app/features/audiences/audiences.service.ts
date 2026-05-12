import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../core/api/api.config';
import {
  GroupRequest,
  GroupView,
  PersonPersonaView,
  PersonRequest,
  PersonView,
  TopicRequest,
  TopicView,
} from './audience.models';

@Injectable({ providedIn: 'root' })
export class AudiencesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  listPersons(): Observable<PersonView[]> {
    return this.http.get<PersonView[]>(`${this.baseUrl}/api/persons`);
  }

  createPerson(body: PersonRequest): Observable<PersonView> {
    return this.http.post<PersonView>(`${this.baseUrl}/api/persons`, body);
  }

  updatePerson(id: string, body: PersonRequest): Observable<PersonView> {
    return this.http.patch<PersonView>(`${this.baseUrl}/api/persons/${id}`, body);
  }

  deletePerson(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/persons/${id}`);
  }

  upsertPersonPersona(personId: string, personaText: string): Observable<PersonPersonaView> {
    return this.http.put<PersonPersonaView>(
      `${this.baseUrl}/api/persons/${personId}/persona`,
      { personaText }
    );
  }

  listGroups(): Observable<GroupView[]> {
    return this.http.get<GroupView[]>(`${this.baseUrl}/api/persongroups`);
  }

  createGroup(body: GroupRequest): Observable<GroupView> {
    return this.http.post<GroupView>(`${this.baseUrl}/api/persongroups`, body);
  }

  updateGroup(id: string, body: GroupRequest): Observable<GroupView> {
    return this.http.patch<GroupView>(`${this.baseUrl}/api/persongroups/${id}`, body);
  }

  deleteGroup(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/persongroups/${id}`);
  }

  listTopics(): Observable<TopicView[]> {
    return this.http.get<TopicView[]>(`${this.baseUrl}/api/topics`);
  }

  createTopic(body: TopicRequest): Observable<TopicView> {
    return this.http.post<TopicView>(`${this.baseUrl}/api/topics`, body);
  }

  updateTopic(id: string, body: TopicRequest): Observable<TopicView> {
    return this.http.patch<TopicView>(`${this.baseUrl}/api/topics/${id}`, body);
  }

  deleteTopic(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/topics/${id}`);
  }
}
