import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';

// Define the component metadata
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['../styles.css']
})

// Define the AppComponent class
export class AppComponent {
  name = '';
  age: string | undefined;
  stats: any[] | undefined;
  maxAge: string | undefined;

  // Inject the HttpClient service
  constructor(private http: HttpClient) {}

  // Define a method to get the age from the server
  getAge() {
    // Make an HTTP GET request to the server with the name parameter
    this.http.get<any>(`http://localhost:8080/?name=${this.name}`).subscribe(data => {
      // Assign the age from the response to the age property
      this.age = data.age;
    });
  }

  // Define a method to clear the age property
  clearAge() {
    this.age = "";
  }

  // Define a method to get the stats from the server
  getStats() {
    this.http.get<any>('http://localhost:8080/stats').subscribe(data => {
      this.stats = Object.entries(data).map(([name, requests]) => ({ name, requests }));
    });
  }

  // Define a method to get the max age from the server
  getMaxAge() {
    this.http.get<any>('http://localhost:8080/max-age-name').subscribe(data => {
      this.maxAge = data.age;
    });
  }

}
