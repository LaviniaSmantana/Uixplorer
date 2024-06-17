import logging
import pandas as pd
import requests
from flask import Flask, request, jsonify
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import atexit

app = Flask(__name__)
logging.basicConfig(level=logging.DEBUG)

session = requests.Session()


# toate articolele din baza de date
def get_articles():
    response = requests.get('http://localhost:2024/api/articles')
    return response.json()


# articolele citite de utilizator
def get_user_articles(user_id):
    response = requests.get(f'http://localhost:2024/api/users/{user_id}/articlesRead')
    return response.json()


# articole citite de toti utilizatorii din tabela user_article
def get_all_user_articles():
    response = requests.get('http://localhost:2024/api/user_articles')
    return response.json()


# vectorizam articolele, luam coloanele care ne intereseaza
def preprocess_articles(articles):
    df = pd.DataFrame(articles)
    df['text'] = (df['title']) * 100 + " " + (df['source']) * 20 + " " + df['textOne'] + " " + df['textTwo'] + " " + df['textThree']
    tfidf = TfidfVectorizer(stop_words='english')
    tfidf_matrix = tfidf.fit_transform(df['text'])
    return df, tfidf_matrix


# calculam similaritatea intre articole
def calculate_similarity(tfidf_matrix):
    return cosine_similarity(tfidf_matrix, tfidf_matrix)


def get_article_indices(user_articles_df, articles_df):
    return articles_df[articles_df['id'].isin(user_articles_df['id'])].index


def get_recommendations(user_articles_df, articles_df, cosine_sim, top_n=3):
    user_article_indices = get_article_indices(user_articles_df, articles_df)
    user_sim_scores = cosine_sim[user_article_indices].mean(axis=0)
    user_sim_scores[user_article_indices] = 0
    recommended_indices = user_sim_scores.argsort()[-top_n:][::-1]
    return articles_df.iloc[recommended_indices]


# top 3 articole citite dintre toti utilizatorii
def get_top_articles(all_user_articles, articles_df, top_n=3):
    article_counts = pd.Series([ua['article']['id'] for ua in all_user_articles]).value_counts()
    top_article_ids = article_counts.index[:top_n]
    return articles_df[articles_df['id'].isin(top_article_ids)]


@app.route('/recommend', methods=['POST'])
def recommend():
    user_id = request.json.get('user_id')
    articles = get_articles()
    user_articles = get_user_articles(user_id)

    articles_df, tfidf_matrix = preprocess_articles(articles)
    cosine_sim = calculate_similarity(tfidf_matrix)

    user_articles_df = pd.DataFrame(user_articles)

    if user_articles_df.empty:
        all_user_articles = get_all_user_articles()
        recommended_articles = get_top_articles(all_user_articles, articles_df, top_n=3)
    else:
        recommended_articles = get_recommendations(user_articles_df, articles_df, cosine_sim, top_n=3)

    return jsonify(recommended_articles.to_dict(orient='records'))


if __name__ == '__main__':
    atexit.register(lambda: session.close())
    app.run(debug=True)
