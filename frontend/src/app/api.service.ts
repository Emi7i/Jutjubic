import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/types/http';
import { Observable } from 'rxjs';

export interface Asset {
  id?: number;
  name?: string;
  [key: string]: any;
}

export interface Person {
  id?: number;
  name: string;
  surname: string;
  email: string;
  jmbg: string;
  age: number;
  dateOfBirth?: string;
  [key: string]: any;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) { }

  getAssets(): Observable<Asset[]> {
    return this.http.get<Asset[]>(`${this.baseUrl}/assets`);
  }

  createPerson(person: Person): Observable<string> {
    return this.http.post<string>(`${this.baseUrl}/person`, person);
  }
}
