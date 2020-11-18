<template>
  <div>
  <base-card>
  <search-input-form
          @update-category-selection="updateCategorySelection($event)"
          @update-book-selection="updateBookSelection($event)"
          @update-chapter-selection="updateChapterSelection($event)"
          @update-search-term-selection="updateSearchTermSelection($event)"
          :chapters-list="chaptersList"
  ></search-input-form>
  </base-card>
<search-results-list
        :search-results="searchResults"
        :display-options="displayOptions"
        @result-selected="sendResultQuery($event)"
></search-results-list>
  </div>
</template>

<script lang = "ts">

/*
  Overview:

    Encapsulates the search & navigation functionality of the app by
    offering the user a search / navigation form, fetching the resulting data from
    the backend API and then displaying a list of the search results.

  Complexity:
    - Handle the emitted data from SearchInputForm (make api call, then pass result as prop to SearchResultsList)

  Parents:
    - Dashboard
// @update-category-selection = "updateCategorySelection($event)"
  Children:
    - SearchInputForm
    - SearchResultsList

  props:
    None

  emits:
    - searchResultSelected(selectedVerse)

  data:
    None

*/

import { Component, Vue,Prop } from 'vue-property-decorator';
import {Book, Chapter, SearchCriteria, Verse} from "@/api/dto";
import SearchInputForm from "@/Components/search/SearchInputForm.vue";
import BaseCard from "@/Components/BaseComponents/BaseCard.vue";
import {TORAH} from "@/api/TANAKH";
import apifiClient from "@/api/apifiClient";
import SearchResultsList from "@/Components/search/SearchResultsList.vue";
import ChapterDisplay from "@/Components/search/ChapterDisplay.vue";


@Component({
  components:{
    SearchInputForm,
    BaseCard,
    SearchResultsList,
    ChapterDisplay

  }
})
export default class SearchController extends Vue{


  public searchCriteria = new SearchCriteria();
  public chaptersList: Chapter[] = [] //will contain the list of chapters in the selected book, to be send down to the form..
  public searchResults: Verse[] = [] //will contain the results of our path search - I.e a chapter of verses...
  public displayOptions = false//we will need to let the searchResults know if it should display links, or we've already done with it, and are now moving to chapter display
  public displayResults = false //if we have a final chapter selected, we should only display the chapter and not the options


    //an api call is made, which populates the chaptersList (with objects of type 'Chapter'), which is passed as a prop to the form.
  public getChapterList(): void{
      this.chaptersList = [];
      apifiClient.findBookByUniquePath(this.searchCriteria.book,
              `{

      chapters{
        hebrewNumeral
        id
        number
        path
      }



            }`).then( res => {
                let i;
                for (i = 0; i < res['data'].findBookByUniquePath.chapters.length; i++) {
                  const ch = new Chapter();
                  ch.hebrewNumeral = res['data'].findBookByUniquePath.chapters[i].hebrewNumeral;
                  ch.id = res['data'].findBookByUniquePath.chapters[i].id;
                  ch.number = res['data'].findBookByUniquePath.chapters[i].number;
                  ch.path = res['data'].findBookByUniquePath.chapters[i].path;
                  this.chaptersList.push(ch);
                }
              });



  }

  public freeTextSearchWithPath(searchPath: string){
    this.searchResults = [] //wipe the searchResults array clean, to get rid of any previous results...
    console.log("searching with path" + searchPath);
    //the api call:
    apifiClient.verseFreeTextSearch({
      customArgs: {
        validPathPrefixes:  [searchPath]
      },
      pageSize: 20,
      pageNumber: 0,
      searchTerm: this.searchCriteria.searchTerm

    }, `{
              content{
           fullHebrewText
           hebrewNumeral
           id
           number
           path
          } }`).then(res => {
      if (res['data'].verseFreeTextSearch.content.length > 0) {
        let i;
        for (i = 0; i < res['data'].verseFreeTextSearch.content.length; i++){
          //console.log(res['data'].verseFreeTextSearch.content[i]);
          this.searchResults.push(res['data'].verseFreeTextSearch.content[i]) ;      }
          console.log(this.searchResults);

      }
      else alert("No results have been found. Please enter a different search term, or expand the search path...")
    });
  }

  public freeTextSearchWithoutPath(): void{
    console.log("Searching without path");
    this.searchResults = []; //wipe the searchResults array clean..
    //the api call:
    apifiClient.verseFreeTextSearch({
      customArgs: {
        validPathPrefixes: ["TORAH", "PROPHETS", "WRITINGS"]
      },
      pageSize: 20,
      pageNumber: 0,
      searchTerm: this.searchCriteria.searchTerm

    }, `{
            content{
           fullHebrewText
           hebrewNumeral
           id
           number
           path
           }}`).then(res => {
              if (res['data'].verseFreeTextSearch.content.length > 0) {
                let i
                for (i = 0; i < res['data'].verseFreeTextSearch.content.length; i++){
                  //console.log(res['data'].verseFreeTextSearch.content[i]);
                this.searchResults.push(res['data'].verseFreeTextSearch.content[i]);}
                //console.log(this.searchResults);
              }
              else alert("No results have been found. Please enter a different search term");
            }

    );
  }

  public freeTextSearchSorter(): void{

    let searchPath = ""
    if(this.searchCriteria.category !== "" && this.searchCriteria.category !== undefined)    //if there is a category selected:
      searchPath = this.searchCriteria.category;

    if(this.searchCriteria.book !== "" && this.searchCriteria.book !== undefined) // if there is a book selected
      searchPath = this.searchCriteria.book;

    if(this.searchCriteria.chapter !== "" && this.searchCriteria.chapter !== undefined) //if there is chapter selected
      searchPath += "/" + this.searchCriteria.chapter;

    if(searchPath === "") //there is no Path to narrow down the FreeTextSearch
      this.freeTextSearchWithoutPath();

    if(searchPath !== "") //there is a path to narrow down the freeTextSearch
      this.freeTextSearchWithPath(searchPath);

  }

  public getChapterFromPathSearch(path: string): void{
    console.log("getting chapter from path");
    this.searchResults = []; //wipe the searchResults array clean...
    //the api call:
    apifiClient.findChapterByUniquePath(path, `{
             hebrewNumeral
    id
    number
    path
    verses{
      fullHebrewText
      hebrewNumeral
      number
      path
    }
            }`).then(res => {
             if(res['data'].findChapterByUniquePath.verses.length > 0) {//making sure there are results
               console.log(res)
               let i;
               for(i = 0; i < res['data'].findChapterByUniquePath.verses.length; i++ ){
                 this.searchResults.push(res['data'].findChapterByUniquePath.verses[i]);
                 //console.log(this.searchResults[i]);
               }
             }
             this.$emit('display-selected-chapter',this.searchResults);
            });
  }

  public generalSearchSorter(): void{
    //if there is a search term, we'll call the free text search function - (which will also make use of the search path..)
      if(this.searchCriteria.searchTerm !== "" && this.searchCriteria.searchTerm !== undefined){
        this.displayResults = false;
        this.displayOptions = true;
        this.$emit('stop-chapter-display',this.displayResults);
        this.freeTextSearchSorter();
      }

      //if there's no search term, we can just search for a chapter based on the path..
      else{
        if(this.searchCriteria.chapter !== "" && this.searchCriteria.chapter !== undefined
                && this.searchCriteria.book !== "" && this.searchCriteria.book !== undefined
                && this.searchCriteria.category !== undefined && this.searchCriteria.category !== "") {
          this.displayOptions = false;
          this.displayResults = true;
          this.getChapterFromPathSearch(this.searchCriteria.book + "/" + this.searchCriteria.chapter.toString());

        }
        else{
          alert("Please enter valid search path or search term...");
        }
      }
  }

  //these functions get the data from the form and update the fields of the searchCriteria object accordingly...
  public updateCategorySelection(selectedCategory: string): void{
    this.searchCriteria.category = selectedCategory;
    //console.log(this.searchCriteria.category + " from controller");

  }

  public updateBookSelection(selectedBook: string): void{
    this.searchCriteria.book = selectedBook;
    //console.log(this.searchCriteria.book + " from controller");
    this.getChapterList();
  }

  public updateChapterSelection(selectedChapter: string): void{
    this.searchCriteria.chapter = selectedChapter;
    //console.log(this.searchCriteria.chapter + " from controller");
  }

  public updateSearchTermSelection(searchTerm: string): void{
    this.searchCriteria.searchTerm = searchTerm;
   // console.log(this.searchCriteria.searchTerm + " from controller")
    this.generalSearchSorter();
  }

  public sendResultQuery(path: string): void{
    this.displayResults = true;
    this.displayOptions = false;
    this.getChapterFromPathSearch(path);

  }


}
</script>

<style scoped>

</style>