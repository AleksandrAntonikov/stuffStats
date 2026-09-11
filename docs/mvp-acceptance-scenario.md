# MVP acceptance scenario

The first usable release is accepted when the following workflow succeeds after an application restart and without an Internet connection.

1. Launch StuffStats.
2. Create an item named `Shoes` with a purchase price of `$100` and a distance metric measured in kilometers.
3. Attach an initial photo and save the item.
4. Record `5 km` of usage.
5. Record another `7 km` of usage.
6. Verify that the item shows `12 km` total and `$8.33/km`.
7. Add a condition photo.
8. Verify that the photo is stored with a `12 km` usage snapshot.
9. Restart the application.
10. Verify that the item, events, statistics, and both photos are unchanged.

The application must remain useful when camera access is denied: selecting an existing photo and tracking usage without photos must still work.
