import { Component } from '@angular/core';
import { ApiService, Person } from './api.service';

@Component({
  selector: 'app-person-form',
  template: `
    <div class="container">
      <h2>Create Person</h2>
      <form (ngSubmit)="submitForm()" #personForm="ngForm">
        <div class="form-group">
          <label for="name">Name:</label>
          <input 
            type="text" 
            id="name" 
            name="name" 
            [(ngModel)]="person.name" 
            required 
            #name="ngModel"
            class="form-control">
          <div *ngIf="name.invalid && name.touched" class="error">
            Name is required
          </div>
        </div>
        
        <div class="form-group">
          <label for="surname">Surname:</label>
          <input 
            type="text" 
            id="surname" 
            name="surname" 
            [(ngModel)]="person.surname" 
            required 
            #surname="ngModel"
            class="form-control">
          <div *ngIf="surname.invalid && surname.touched" class="error">
            Surname is required
          </div>
        </div>
        
        <div class="form-group">
          <label for="email">Email:</label>
          <input 
            type="email" 
            id="email" 
            name="email" 
            [(ngModel)]="person.email" 
            required 
            email
            #email="ngModel"
            class="form-control">
          <div *ngIf="email.invalid && email.touched" class="error">
            Valid email is required
          </div>
        </div>
        
        <div class="form-group">
          <label for="jmbg">JMBG (13 digits):</label>
          <input 
            type="text" 
            id="jmbg" 
            name="jmbg" 
            [(ngModel)]="person.jmbg" 
            required 
            pattern="[0-9]{13}"
            #jmbg="ngModel"
            class="form-control">
          <div *ngIf="jmbg.invalid && jmbg.touched" class="error">
            JMBG must be exactly 13 digits
          </div>
        </div>
        
        <div class="form-group">
          <label for="age">Age:</label>
          <input 
            type="number" 
            id="age" 
            name="age" 
            [(ngModel)]="person.age" 
            required 
            min="18"
            #age="ngModel"
            class="form-control">
          <div *ngIf="age.invalid && age.touched" class="error">
            Age must be at least 18
          </div>
        </div>
        
        <button type="submit" [disabled]="personForm.invalid || submitting" class="btn-submit">
          {{ submitting ? 'Creating...' : 'Create Person' }}
        </button>
        
        <button type="button" (click)="resetForm()" class="btn-reset">Reset</button>
      </form>
      
      <div *ngIf="successMessage" class="success">{{ successMessage }}</div>
      <div *ngIf="errorMessage" class="error">{{ errorMessage }}</div>
    </div>
  `,
  styles: [`
    .container {
      padding: 20px;
      max-width: 500px;
      margin: 0 auto;
    }
    .form-group {
      margin-bottom: 15px;
    }
    label {
      display: block;
      margin-bottom: 5px;
      font-weight: bold;
    }
    .form-control {
      width: 100%;
      padding: 8px;
      border: 1px solid #ddd;
      border-radius: 4px;
      box-sizing: border-box;
    }
    .btn-submit {
      background: #28a745;
      color: white;
      border: none;
      padding: 10px 20px;
      border-radius: 4px;
      cursor: pointer;
      margin-right: 10px;
    }
    .btn-submit:disabled {
      background: #6c757d;
      cursor: not-allowed;
    }
    .btn-reset {
      background: #6c757d;
      color: white;
      border: none;
      padding: 10px 20px;
      border-radius: 4px;
      cursor: pointer;
    }
    .error {
      color: red;
      background: #ffebee;
      padding: 10px;
      border-radius: 4px;
      margin: 10px 0;
      font-size: 14px;
    }
    .success {
      color: #155724;
      background: #d4edda;
      padding: 10px;
      border-radius: 4px;
      margin: 10px 0;
    }
  `]
})
export class PersonFormComponent {
  person: Person = {
    name: '',
    surname: '',
    email: '',
    jmbg: '',
    age: 18
  };
  submitting = false;
  successMessage: string | null = null;
  errorMessage: string | null = null;

  constructor(private apiService: ApiService) {}

  submitForm(): void {
    this.submitting = true;
    this.successMessage = null;
    this.errorMessage = null;

    this.apiService.createPerson(this.person).subscribe({
      next: () => {
        this.successMessage = 'Person created successfully!';
        this.resetForm();
        this.submitting = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to create person: ' + err.message;
        this.submitting = false;
        console.error('Error creating person:', err);
      }
    });
  }

  resetForm(): void {
    this.person = {
      name: '',
      surname: '',
      email: '',
      jmbg: '',
      age: 18
    };
    this.successMessage = null;
    this.errorMessage = null;
  }
}
